import { toast } from 'sonner'
import { SimulatedBadge } from '@/components/shared/simulated-badge'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { mensagemDeErro } from '@/lib/cadastro'
import { useDefinirWhatsapp, usePreferencias } from '../hooks'

/** Opt-in do WhatsApp (simulado). Ligar grava o consentimento; vale para as próximas notificações. */
export function PreferenciaWhatsapp() {
  const preferencias = usePreferencias()
  const definir = useDefinirWhatsapp()
  return (
    <div className="flex flex-wrap items-center gap-2">
      <Switch
        id="pref-whatsapp"
        checked={preferencias.data?.whatsapp ?? false}
        disabled={!preferencias.data || definir.isPending}
        onCheckedChange={(valor) =>
          definir.mutate(valor, {
            onSuccess: (p) => toast.success(p.whatsapp ? 'WhatsApp ligado.' : 'WhatsApp desligado.'),
            onError: (e) => toast.error(mensagemDeErro(e, 'Não foi possível salvar a preferência.')),
          })
        }
      />
      <Label htmlFor="pref-whatsapp">Receber por WhatsApp</Label>
      <SimulatedBadge />
    </div>
  )
}
