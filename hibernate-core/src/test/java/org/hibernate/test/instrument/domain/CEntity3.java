package org.hibernate.test.instrument.domain;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@javax.persistence.Entity(name="CEntity")
public class CEntity3 {

	@Id
	public int id;

	public CEntity3(int id) {
		this();
		this.id = id;
		bEntities = new LinkedHashSet();
	}

	public CEntity3() {
		// this form used by Hibernate
	}

	public Set<BEntity3> getBEntities() {
		return bEntities;
	}

	@OneToMany(mappedBy="cEntity", fetch=FetchType.LAZY)
	public Set<BEntity3> bEntities = null;
}
