import { useEffect, useEffectEvent, useRef, useState } from 'react'
import { ApiError } from '@/lib/api'
import { localizacaoApi } from './api'
import { LOCALIZACAO } from './limites'
import type { Posicao } from './types'

/** Situação do GPS do aparelho. A viagem continua em andamento em todas (só o servidor encerra). */
export type EstadoGps = 'aguardando' | 'transmitindo' | 'negada' | 'sem-sinal' | 'sem-suporte'

export type Transmissao = {
  estado: EstadoGps
  /** Hora (epoch ms) da última posição aceita pelo servidor neste aparelho. */
  ultimaEnviadaEm: number | null
  /** Sem resposta do servidor (sem internet): a fila espera. */
  semConexao: boolean
  /** O servidor respondeu com erro passageiro (ex.: 500): a fila espera e tenta de novo. */
  falhaEnvio: boolean
  /** O último lote voltou sem nenhuma aceita e com rejeitadas (ex.: relógio do celular errado). */
  recusadas: boolean
  naFila: number
}

const NAO_EM_ANDAMENTO = 'A viagem não está em andamento.'

/** Só param a transmissão: viagem fora de andamento, de outro motorista ou inexistente. O resto é passageiro. */
function encerraTransmissao(e: unknown): e is ApiError {
  return e instanceof ApiError && (e.status === 403 || e.status === 404 || (e.status === 400 && e.message === NAO_EM_ANDAMENTO))
}

const ENVIO_MS = LOCALIZACAO.envioSegundos * 1000
/** De quanto em quanto tempo confere se já pode enviar (o envio em si respeita ENVIO_MS). */
const CONFERENCIA_MS = 5000

/**
 * Transmite a posição do celular do motorista enquanto a viagem está em andamento: watchPosition com alta precisão,
 * no máximo uma posição (a mais recente) a cada 15 s, fila de até 20 sem internet (descarta a mais antiga e as velhas
 * demais) e Wake Lock quando o navegador tiver. Resposta 4xx do servidor (viagem não está mais em andamento) para a
 * transmissão e chama {@code aoParar}.
 */
export function useTransmissao(viagemId: string, aoParar: (mensagem: string) => void): Transmissao {
  const [estado, setEstado] = useState<EstadoGps>(() => ('geolocation' in navigator ? 'aguardando' : 'sem-suporte'))
  const [ultimaEnviadaEm, setUltimaEnviadaEm] = useState<number | null>(null)
  const [semConexao, setSemConexao] = useState(false)
  const [falhaEnvio, setFalhaEnvio] = useState(false)
  const [recusadas, setRecusadas] = useState(false)
  const [naFila, setNaFila] = useState(0)
  const parar = useEffectEvent(aoParar)
  const fila = useRef<Posicao[]>([])
  const maisRecente = useRef<Posicao | null>(null)

  useEffect(() => {
    if (!('geolocation' in navigator)) return
    let ativa = true
    let enviando = false
    let ultimoEnvio = 0

    /** A mais recente entra na fila; fica só o que o servidor ainda aceitaria, no máximo 20. */
    const prepararFila = () => {
      if (maisRecente.current) fila.current = [...fila.current, maisRecente.current].slice(-LOCALIZACAO.filaMax)
      maisRecente.current = null
      const limite = Date.now() - LOCALIZACAO.posicaoMaxIdadeSegundos * 1000
      fila.current = fila.current.filter((p) => Date.parse(p.registradaEm) >= limite)
      setNaFila(fila.current.length)
    }

    const enviar = async () => {
      if (enviando || !ativa) return
      prepararFila()
      if (fila.current.length === 0) return
      enviando = true
      ultimoEnvio = Date.now()
      const lote = fila.current
      try {
        const resultado = await localizacaoApi.enviar(viagemId, lote)
        if (!ativa) return
        // Respondido pelo servidor (aceitas, rejeitadas ou ignoradas): o lote sai da fila.
        fila.current = fila.current.filter((p) => !lote.includes(p))
        if (resultado.aceitas > 0) setUltimaEnviadaEm(Math.max(...lote.map((p) => Date.parse(p.registradaEm))))
        setRecusadas(resultado.aceitas === 0 && resultado.rejeitadas > 0)
        setSemConexao(false)
        setFalhaEnvio(false)
      } catch (e) {
        if (!ativa) return
        if (encerraTransmissao(e)) {
          ativa = false
          fila.current = []
          parar(e.message)
        } else if (e instanceof ApiError && e.status === 0) {
          setSemConexao(true)
        } else {
          setFalhaEnvio(true)
        }
      } finally {
        enviando = false
        setNaFila(fila.current.length)
      }
    }

    const enviarSePuder = () => {
      if (Date.now() - ultimoEnvio >= ENVIO_MS) void enviar()
    }

    const vigia = navigator.geolocation.watchPosition(
      (p) => {
        // Hora com fuso (UTC, "Z"): o servidor converte para o fuso da operação. Aqui toISOString é o certo.
        maisRecente.current = {
          latitude: p.coords.latitude,
          longitude: p.coords.longitude,
          precisaoM: Math.min(p.coords.accuracy, 5000),
          registradaEm: new Date(p.timestamp).toISOString(),
        }
        setEstado('transmitindo')
        enviarSePuder()
      },
      (erro) => setEstado(erro.code === erro.PERMISSION_DENIED ? 'negada' : 'sem-sinal'),
      { enableHighAccuracy: true, maximumAge: 0, timeout: 30_000 },
    )
    const conferencia = setInterval(enviarSePuder, CONFERENCIA_MS)
    const aoVoltarInternet = () => void enviar()
    window.addEventListener('online', aoVoltarInternet)
    return () => {
      ativa = false
      navigator.geolocation.clearWatch(vigia)
      clearInterval(conferencia)
      window.removeEventListener('online', aoVoltarInternet)
    }
  }, [viagemId])

  useManterTelaLigada()

  return { estado, ultimaEnviadaEm, semConexao, falhaEnvio, recusadas, naFila }
}

/** Wake Lock: pede de novo quando a página volta a ficar visível. Sem suporte, nada muda. */
function useManterTelaLigada() {
  useEffect(() => {
    if (!('wakeLock' in navigator)) return
    let trava: WakeLockSentinel | null = null
    let ativa = true
    const pedir = async () => {
      try {
        const nova = await navigator.wakeLock.request('screen')
        if (ativa) trava = nova
        else void nova.release()
      } catch {
        trava = null // negado (bateria fraca, aba oculta): a transmissão segue sem ele
      }
    }
    const aoMostrar = () => {
      if (document.visibilityState === 'visible') void pedir()
    }
    void pedir()
    document.addEventListener('visibilitychange', aoMostrar)
    return () => {
      ativa = false
      document.removeEventListener('visibilitychange', aoMostrar)
      void trava?.release()
    }
  }, [])
}
