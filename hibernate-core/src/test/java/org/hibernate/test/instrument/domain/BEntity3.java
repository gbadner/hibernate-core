package org.hibernate.test.instrument.domain;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@javax.persistence.Entity(name="BEntity")
public class BEntity3 {
	@Id
	public int id;

	@ManyToOne(fetch= FetchType.LAZY)
	@LazyToOne(LazyToOneOption.NO_PROXY )
	public AEntity3 aEntity = null;

	@ManyToOne(fetch=FetchType.LAZY, cascade = CascadeType.ALL)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	public CEntity3 cEntity = null;

	public BEntity3() {
	}

	public BEntity3(int id) {
		this.id = id;
	}
}
