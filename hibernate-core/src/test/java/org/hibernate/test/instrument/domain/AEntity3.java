package org.hibernate.test.instrument.domain;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.*;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@javax.persistence.Entity(name="AEntity")
public class AEntity3 {

	@Id
	public int id;

	@OneToMany(mappedBy="aEntity", fetch= FetchType.LAZY, cascade = CascadeType.ALL)
	public Set<BEntity3> bEntities = new HashSet<>();

	public AEntity3() {
	}

	public AEntity3(int id) {
		this.id = id;
	}

}
