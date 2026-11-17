import { useParams } from 'react-router'
import { PageHeader } from '@/components/shared/page-header'
import { RegistroLoader } from '@/components/shared/registro-loader'
import { RotaForm } from '@/features/rotas/components/rota-form'
import { useRota, useSalvarRota } from '@/features/rotas/hooks'
import { paraFormulario, paraRequisicao } from '@/features/rotas/schemas'
import { useAcoesCadastro } from '@/lib/cadastro'

export function RotaFormPage() {
  const { id } = useParams()
  const rota = useRota(id)
  const acoes = useAcoesCadastro({
    id,
    lista: '/cadastros/rotas',
    mutacao: useSalvarRota(id),
    paraRequisicao,
    paraFormulario,
    mensagemSucesso: 'Rota salva.',
  })

  return (
    <>
      <PageHeader title={id ? 'Editar rota' : 'Nova rota'} />
      <RegistroLoader edicao={!!id} consulta={rota} naoEncontrado="Rota não encontrada.">
        <RotaForm key={rota.data?.id ?? 'nova'} defaultValues={paraFormulario(rota.data)} {...acoes} />
      </RegistroLoader>
    </>
  )
}
