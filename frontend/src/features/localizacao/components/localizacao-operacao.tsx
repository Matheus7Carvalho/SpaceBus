import { ErrorState } from '@/components/shared/error-state'
import { Skeleton } from '@/components/ui/skeleton'
import type { Viagem } from '@/features/viagens/types'
import { useLocalizacaoViagem } from '../hooks'
import { DiagnosticoLocalizacao } from './diagnostico-localizacao'
import { PainelLocalizacao } from './painel-localizacao'

/** Bloco Localização da edição da viagem (em andamento ou encerrada): posição, avisos e qualidade do sinal. */
export function LocalizacaoOperacao({ viagem }: { viagem: Viagem }) {
  const consulta = useLocalizacaoViagem(viagem.id)
  const dados = consulta.data

  return (
    <section aria-labelledby="localizacao-titulo" className="grid max-w-4xl gap-3">
      <h2 id="localizacao-titulo" className="text-base font-semibold">
        Localização
      </h2>
      {consulta.isPending && <Skeleton className="h-24 w-full" />}
      {!dados && consulta.isError && <ErrorState message="Não foi possível carregar a localização." onRetry={() => consulta.refetch()} />}
      {dados && (
        <>
          {dados.semMotorista && dados.situacaoViagem === 'EM_ANDAMENTO' && (
            <p role="status" className="rounded-lg bg-warning/15 px-3 py-2 text-sm">
              Sem motorista: localização indisponível
            </p>
          )}
          <PainelLocalizacao localizacao={dados} falhaAoAtualizar={consulta.isError} />
          <DiagnosticoLocalizacao diagnostico={dados.diagnostico} />
        </>
      )}
    </section>
  )
}
