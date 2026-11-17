import { useParams } from 'react-router'
import { PageHeader } from '@/components/shared/page-header'
import { RegistroLoader } from '@/components/shared/registro-loader'
import { VeiculoForm } from '@/features/veiculos/components/veiculo-form'
import { useSalvarVeiculo, useVeiculo } from '@/features/veiculos/hooks'
import { paraFormulario, paraRequisicao } from '@/features/veiculos/schemas'
import { useAcoesCadastro } from '@/lib/cadastro'

export function VeiculoFormPage() {
  const { id } = useParams()
  const veiculo = useVeiculo(id)
  const acoes = useAcoesCadastro({
    id,
    lista: '/cadastros/veiculos',
    mutacao: useSalvarVeiculo(id),
    paraRequisicao,
    paraFormulario,
    mensagemSucesso: 'Veículo salvo.',
  })

  return (
    <>
      <PageHeader title={id ? 'Editar veículo' : 'Novo veículo'} />
      <RegistroLoader edicao={!!id} consulta={veiculo} naoEncontrado="Veículo não encontrado.">
        <VeiculoForm key={veiculo.data?.id ?? 'novo'} defaultValues={paraFormulario(veiculo.data)} {...acoes} />
      </RegistroLoader>
    </>
  )
}
