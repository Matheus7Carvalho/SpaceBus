package com.spacebus.ops.domain.entity;

import com.spacebus.ops.domain.core.entity.BaseEntity;
import com.spacebus.ops.domain.entity.enums.TipoRota;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ROTA")
public class Rota extends BaseEntity {

    @Column(name = "NOME", nullable = false, length = 100)
    private String nome;

    @Column(name = "ORIGEM", nullable = false, length = 100)
    private String origem;

    @Column(name = "DESTINO", nullable = false, length = 100)
    private String destino;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 5)
    private TipoRota tipo = TipoRota.UNICA;

    /** Só na Rota Fixa. */
    @OneToOne(mappedBy = "rota", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Rotina rotina;

    /** Pontos sempre acompanham a rota; SUBSELECT evita N+1 na listagem. */
    @OneToMany(mappedBy = "rota", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("ordem ASC")
    private List<PontoParada> pontos = new ArrayList<>();

    public Rota(String nome, String origem, String destino, List<PontoParada> pontos) {
        this.nome = nome;
        this.origem = origem;
        this.destino = destino;
        substituirPontos(pontos);
    }

    /** Liga a rotina à rota (lado dono da relação) ou remove (null). */
    public void definirRotina(Rotina nova) {
        if (nova != null) nova.setRota(this);
        this.rotina = nova;
    }

    /** Substitui os pontos mantendo a coleção gerenciada pelo Hibernate (orphanRemoval). */
    public void substituirPontos(List<PontoParada> novos) {
        pontos.clear();
        if (novos == null) return;
        for (PontoParada ponto : novos) {
            ponto.setRota(this);
            pontos.add(ponto);
        }
    }
}
