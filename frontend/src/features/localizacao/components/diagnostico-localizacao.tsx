import { formatarDataHora } from '@/lib/format'
import { LOCALIZACAO } from '../limites'
import { motivoRejeicaoLabel, type Diagnostico } from '../types'

/** "1 min 15 s", "40 s". */
function formatarDuracao(segundos: number) {
  const min = Math.floor(segundos / 60)
  const s = segundos % 60
  if (min === 0) return `${s} s`
  return s === 0 ? `${min} min` : `${min} min ${s} s`
}

/** Qualidade do sinal para a operação agir quando a localização falhar. Sem coordenadas. */
export function DiagnosticoLocalizacao({ diagnostico: d }: { diagnostico: Diagnostico }) {
  const rejeitadas = d.rejeitadasAdiantada + d.rejeitadasAntiga + d.rejeitadasImprecisa
  const porMotivo = (
    [
      ['ADIANTADA', d.rejeitadasAdiantada],
      ['ANTIGA', d.rejeitadasAntiga],
      ['IMPRECISA', d.rejeitadasImprecisa],
    ] as const
  )
    .filter(([, n]) => n > 0)
    .map(([motivo, n]) => `${motivoRejeicaoLabel[motivo]} ${n}`)
    .join(' · ')

  const linhas: [string, string][] = [
    ['Início', formatarDataHora(d.iniciadaEm)],
    ...(d.encerradaEm ? ([['Encerramento', formatarDataHora(d.encerradaEm)]] as [string, string][]) : []),
    ['Última posição recebida', formatarDataHora(d.ultimaRecebidaEm)],
    ['Aceitas', String(d.aceitas)],
    ['Rejeitadas', rejeitadas === 0 ? '0' : `${rejeitadas} (${porMotivo})`],
    [`Lacunas acima de ${LOCALIZACAO.lacunaSegundos} s`, String(d.lacunas)],
    ['Maior intervalo', formatarDuracao(d.maiorIntervaloSegundos)],
  ]

  return (
    <dl aria-label="Diagnóstico do sinal" className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 rounded-lg border p-3 text-sm sm:grid-cols-[auto_1fr_auto_1fr]">
      {linhas.map(([rotulo, valor]) => (
        <div key={rotulo} className="contents">
          <dt className="text-muted-foreground">{rotulo}</dt>
          <dd className="font-medium tabular-nums">{valor}</dd>
        </div>
      ))}
    </dl>
  )
}
