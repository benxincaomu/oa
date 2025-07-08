package io.github.benxincaomu.oa.bussiness.workflow;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@Entity
@SequenceGenerator(name = "test_table_seq", sequenceName = "test_table_seq", allocationSize = 1)

public class TestTable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_table_seq")

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
