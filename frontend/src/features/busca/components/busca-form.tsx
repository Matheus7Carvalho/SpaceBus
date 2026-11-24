import { ArrowRightLeftIcon, SearchIcon } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { LimitedInput } from '@/components/shared/limited-input'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { useLocais } from '../hooks'
import { LIMITE } from '../limites'
import type { Busca } from '../types'

type BuscaFormProps = {
  inicial: Busca
  /** Menor data aceita ("AAAA-MM-DD", hoje). */
  minData: string
  onBuscar: (busca: Busca) => void
}

/** Origem, destino e data. Os campos sugerem os locais atendidos (datalist). */
export function BuscaForm({ inicial, minData, onBuscar }: BuscaFormProps) {
  const [busca, setBusca] = useState(inicial)
  const locais = useLocais()
  const definir = (campo: keyof Busca) => (valor: string) => setBusca((b) => ({ ...b, [campo]: valor }))

  const enviar = (e: FormEvent) => {
    e.preventDefault()
    onBuscar({ ...busca, origem: busca.origem.trim(), destino: busca.destino.trim() })
  }

  return (
    <form role="search" aria-label="Buscar viagens" onSubmit={enviar} className="grid gap-3 rounded-lg border p-4 md:grid-cols-[1fr_auto_1fr_11rem_auto] md:items-end">
      <Field className="gap-1.5">
        <FieldLabel htmlFor="busca-origem">Origem</FieldLabel>
        <LimitedInput
          id="busca-origem"
          required
          maxLength={LIMITE.local}
          length={busca.origem.length}
          list="busca-locais"
          autoComplete="off"
          placeholder="De onde você sai"
          value={busca.origem}
          onChange={(e) => definir('origem')(e.target.value)}
        />
      </Field>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        aria-label="Inverter origem e destino"
        className="justify-self-center"
        onClick={() => setBusca((b) => ({ ...b, origem: b.destino, destino: b.origem }))}
      >
        <ArrowRightLeftIcon aria-hidden className="rotate-90 md:rotate-0" />
      </Button>
      <Field className="gap-1.5">
        <FieldLabel htmlFor="busca-destino">Destino</FieldLabel>
        <LimitedInput
          id="busca-destino"
          required
          maxLength={LIMITE.local}
          length={busca.destino.length}
          list="busca-locais"
          autoComplete="off"
          placeholder="Para onde você vai"
          value={busca.destino}
          onChange={(e) => definir('destino')(e.target.value)}
        />
      </Field>
      <Field className="gap-1.5">
        <FieldLabel htmlFor="busca-data">Data</FieldLabel>
        <Input id="busca-data" type="date" required min={minData} value={busca.data} onChange={(e) => definir('data')(e.target.value)} />
      </Field>
      <Button type="submit">
        <SearchIcon aria-hidden />
        Buscar
      </Button>
      <datalist id="busca-locais">
        {(locais.data ?? []).map((local) => (
          <option key={local} value={local} />
        ))}
      </datalist>
    </form>
  )
}
