import { useParams } from 'react-router'
import { PageHeader } from '@/components/shared/page-header'
import { RegistroLoader } from '@/components/shared/registro-loader'
import { ViagemForm } from '@/features/viagens/components/viagem-form'
import { useSalvarViagem, useViagem } from '@/features/viagens/hooks'
import { paraFormulario, paraRequisicao } from '@/features/viagens/schemas'
import { useAcoesCadastro } from '@/lib/cadastro'

export function ViagemFormPage() {
  const { id } = useParams()
  const viagem = useViagem(id)
  const acoes = useAcoesCadastro({
    id,
    lista: '/operacao/viagens',
    mutacao: useSalvarViagem(id),
    paraRequisicao,
    paraFormulario,
    mensagemSucesso: 'Viagem salva.',
  })

  return (
    <>
      <PageHeader title={id ? 'Editar viagem' : 'Nova viagem'} />
      <RegistroLoader edicao={!!id} consulta={viagem} naoEncontrado="Viagem não encontrada.">
        <ViagemForm key={viagem.data?.id ?? 'nova'} viagem={viagem.data} defaultValues={paraFormulario(viagem.data)} {...acoes} />
      </RegistroLoader>
    </>
  )
}
