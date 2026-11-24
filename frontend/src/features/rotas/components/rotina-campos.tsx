import { PlusIcon, Trash2Icon } from 'lucide-react'
import { Controller, useFieldArray, useFormContext, useWatch } from 'react-hook-form'
import { SelectField } from '@/components/shared/select-field'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { useVeiculos } from '@/features/veiculos/hooks'
import { formatarPlaca } from '@/features/veiculos/types'
import { useMotoristas } from '@/features/viagens/hooks'
import { LIMITE as LIMITE_VIAGEM } from '@/features/viagens/limites'
import { SEM_MOTORISTA } from '@/features/viagens/schemas'
import { LIMITE } from '../limites'
import type { RotaForm } from '../schemas'
import { diasSemana, frequencias } from '../types'

type Props = {
  /** Veículo já gravado na rotina (continua na lista mesmo se inativo). */
  veiculoAtual?: string
}

/** Rotina da Rota Fixa: quando (frequência, dias, horários, vigência) e como (veículo, preço, capacidade). */
export function RotinaCampos({ veiculoAtual }: Props) {
  const { control, register, formState } = useFormContext<RotaForm>()
  const erros = formState.errors.rotina
  const frequencia = useWatch({ control, name: 'rotina.frequencia' })
  const veiculoId = useWatch({ control, name: 'rotina.veiculoId' })
  const horarios = useFieldArray({ control, name: 'rotina.horarios', keyName: 'chave' })
  const veiculos = useVeiculos()
  const motoristas = useMotoristas()
  const cheio = horarios.fields.length >= LIMITE.horariosMax
  const veiculo = veiculos.data?.find((v) => v.id === veiculoId)
  const erroHorarios = erros?.horarios?.root ?? erros?.horarios

  return (
    <section aria-labelledby="rotina-titulo" className="grid gap-4 rounded-lg border p-4">
      <h2 id="rotina-titulo" className="text-base font-semibold">
        Rotina
      </h2>

      <div className="grid gap-4 md:grid-cols-[12rem_1fr]">
        <SelectField<RotaForm> name="rotina.frequencia" id="rotina-frequencia" label="Frequência" opcoes={frequencias} />
        {frequencia === 'SEMANAL' && (
          <Controller
            control={control}
            name="rotina.dias"
            render={({ field }) => (
              <fieldset className="grid gap-1.5" aria-invalid={!!erros?.dias}>
                <legend className="mb-1.5 text-sm font-medium">Dias da semana</legend>
                <div className="flex flex-wrap gap-3">
                  {diasSemana.map((d) => (
                    <label key={d.valor} className="flex items-center gap-1.5 text-sm">
                      <Checkbox
                        checked={field.value.includes(d.valor)}
                        aria-label={d.rotulo}
                        onCheckedChange={(marcado) =>
                          field.onChange(marcado ? [...field.value, d.valor] : field.value.filter((v) => v !== d.valor))
                        }
                      />
                      <span aria-hidden>{d.curto}</span>
                    </label>
                  ))}
                </div>
                <FieldError errors={[erros?.dias]} />
              </fieldset>
            )}
          />
        )}
      </div>

      <fieldset className="grid gap-2">
        <div className="flex items-center justify-between gap-2">
          <legend className="text-sm font-medium">
            Horários de partida{' '}
            <span className="font-normal text-muted-foreground">
              ({horarios.fields.length}/{LIMITE.horariosMax})
            </span>
          </legend>
          <Button type="button" variant="outline" size="sm" disabled={cheio} onClick={() => horarios.append({ valor: '' }, { focusName: `rotina.horarios.${horarios.fields.length}.valor` })}>
            <PlusIcon aria-hidden />
            Adicionar horário
          </Button>
        </div>
        <div className="flex flex-wrap gap-2">
          {horarios.fields.map((h, i) => (
            <div key={h.chave} className="flex items-center gap-1">
              <Input type="time" required aria-label={`Horário ${i + 1}`} className="w-32" {...register(`rotina.horarios.${i}.valor`)} />
              <Button type="button" variant="ghost" size="icon-sm" aria-label={`Remover horário ${i + 1}`} disabled={horarios.fields.length <= 1} onClick={() => horarios.remove(i)}>
                <Trash2Icon aria-hidden />
              </Button>
            </div>
          ))}
        </div>
        <FieldError errors={[erroHorarios?.message ? { message: erroHorarios.message } : undefined]} />
      </fieldset>

      <div className="grid gap-4 md:grid-cols-2">
        <SelectField<RotaForm>
          name="rotina.veiculoId"
          id="rotina-veiculo"
          label="Veículo"
          opcoes={(veiculos.data ?? [])
            .filter((v) => v.ativo || v.id === veiculoAtual)
            .map((v) => ({ valor: v.id, rotulo: `${formatarPlaca(v.placa)} · ${v.descricao} · ${v.capacidade} lugares` }))}
          placeholder={veiculos.isPending ? 'Carregando…' : 'Selecione'}
        />
        <SelectField<RotaForm>
          name="rotina.motoristaId"
          id="rotina-motorista"
          label="Motorista"
          opcoes={[{ valor: SEM_MOTORISTA, rotulo: 'A definir' }, ...(motoristas.data ?? []).map((m) => ({ valor: m.id, rotulo: m.nome }))]}
        />
      </div>

      <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-4">
        <Field data-invalid={!!erros?.preco} className="gap-1.5">
          <FieldLabel htmlFor="rotina-preco">Preço (R$)</FieldLabel>
          <Input
            id="rotina-preco"
            type="number"
            required
            inputMode="decimal"
            min={LIMITE_VIAGEM.precoMin}
            max={LIMITE_VIAGEM.precoMax}
            step={0.01}
            aria-invalid={!!erros?.preco}
            {...register('rotina.preco', { valueAsNumber: true })}
          />
          <FieldError errors={[erros?.preco]} />
        </Field>
        <Field data-invalid={!!erros?.capacidade} className="gap-1.5">
          <FieldLabel htmlFor="rotina-capacidade">Capacidade (lugares)</FieldLabel>
          <Input
            id="rotina-capacidade"
            type="number"
            required
            min={LIMITE_VIAGEM.capacidadeMin}
            max={veiculo?.capacidade ?? LIMITE_VIAGEM.capacidadeMax}
            step={1}
            aria-invalid={!!erros?.capacidade}
            {...register('rotina.capacidade', { valueAsNumber: true })}
          />
          <FieldError errors={[erros?.capacidade]} />
        </Field>
        <Field data-invalid={!!erros?.inicio} className="gap-1.5">
          <FieldLabel htmlFor="rotina-inicio">Início</FieldLabel>
          <Input id="rotina-inicio" type="date" required aria-invalid={!!erros?.inicio} {...register('rotina.inicio')} />
          <FieldError errors={[erros?.inicio]} />
        </Field>
        <Field data-invalid={!!erros?.fim} className="gap-1.5">
          <FieldLabel htmlFor="rotina-fim">Fim (opcional)</FieldLabel>
          <Input id="rotina-fim" type="date" aria-invalid={!!erros?.fim} {...register('rotina.fim')} />
          <FieldError errors={[erros?.fim]} />
        </Field>
      </div>

      <Controller
        control={control}
        name="rotina.publicar"
        render={({ field }) => (
          <Field orientation="horizontal" className="gap-2">
            <Checkbox id="rotina-publicar" checked={field.value} onCheckedChange={(v) => field.onChange(v === true)} onBlur={field.onBlur} />
            <FieldLabel htmlFor="rotina-publicar">Publicar automaticamente as viagens geradas</FieldLabel>
          </Field>
        )}
      />
    </section>
  )
}
