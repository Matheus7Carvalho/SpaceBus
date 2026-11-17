package com.spacebus.ops.domain.entity;

import com.spacebus.ops.domain.core.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
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
