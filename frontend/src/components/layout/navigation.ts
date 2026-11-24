import { BellIcon, BusIcon, CalendarClockIcon, NavigationIcon, RouteIcon, SearchIcon, TicketIcon, type LucideIcon } from 'lucide-react'
import { temPerfil } from '@/features/auth/permissoes'
import type { Perfil } from '@/features/auth/types'

export type NavItem = {
  title: string
  to: string
  icon: LucideIcon
  /** Ausente: todos os perfis. */
  perfis?: readonly Perfil[]
}

export type NavGrupo = { titulo: string; itens: readonly NavItem[] }

/** Mantêm frota, rotas e viagens da transportadora. Igual a OPERACAO no SecurityConfiguration. */
const OPERACAO = ['ADMINISTRADOR', 'OPERADOR'] as const satisfies readonly Perfil[]

/**
 * Só entram aqui módulos já implementados. Perfis iguais aos do backend (SecurityConfiguration).
 * A ordem define o destino inicial: cada perfil cai no primeiro item permitido.
 *
 * Fluxo B do escopo (docs/escopo-mvp.md): cadastra veículo e rota, depois programa e publica viagens.
 * Fluxo A: o passageiro busca e abre o detalhe da viagem.
 */
export const navGrupos: readonly NavGrupo[] = [
  {
    // Primeiro: é a tela inicial do motorista (os outros perfis não veem o grupo).
    titulo: 'Motorista',
    itens: [{ title: 'Minhas viagens', to: '/motorista/viagens', icon: NavigationIcon, perfis: ['MOTORISTA'] }],
  },
  {
    titulo: 'Operação',
    itens: [{ title: 'Viagens', to: '/operacao/viagens', icon: CalendarClockIcon, perfis: OPERACAO }],
  },
  {
    titulo: 'Cadastros',
    itens: [
      { title: 'Rotas', to: '/cadastros/rotas', icon: RouteIcon, perfis: OPERACAO },
      { title: 'Veículos', to: '/cadastros/veiculos', icon: BusIcon, perfis: OPERACAO },
    ],
  },
  {
    // Por último: a operação continua sendo a tela inicial de Operador e Administrador.
    titulo: 'Passageiro',
    itens: [
      { title: 'Buscar viagens', to: '/buscar', icon: SearchIcon },
      { title: 'Minhas reservas', to: '/reservas', icon: TicketIcon, perfis: ['PASSAGEIRO'] },
      { title: 'Notificações', to: '/notificacoes', icon: BellIcon, perfis: ['PASSAGEIRO'] },
    ],
  },
]

const todosItens = navGrupos.flatMap((g) => g.itens)

/** Grupos visíveis ao perfil (grupos sem itens permitidos somem). */
export function gruposPermitidos(perfil: Perfil | undefined): NavGrupo[] {
  return navGrupos
    .map((g) => ({ ...g, itens: g.itens.filter((i) => temPerfil(perfil, i.perfis)) }))
    .filter((g) => g.itens.length > 0)
}

export function itensPermitidos(perfil: Perfil | undefined): readonly NavItem[] {
  return todosItens.filter((item) => temPerfil(perfil, item.perfis))
}

/** Item de menu que cobre a rota (inclui subrotas, ex.: /cadastros/rotas/nova). */
export function itemDaRota(to: string): NavItem | undefined {
  return todosItens.find((item) => to === item.to || to.startsWith(`${item.to}/`))
}

/**
 * Pode o perfil acessar a rota? Rota fora do menu: negado por padrão.
 * Item sem `perfis`: liberado a todos os perfis autenticados.
 */
export function podeAcessar(perfil: Perfil | undefined, to: string): boolean {
  const item = itemDaRota(to)
  return !!item && temPerfil(perfil, item.perfis)
}
