package org.hibernate.test.instrument.domain;

import java.sql.Blob;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name="D")
public class DEntity {
	
	// ****** ID *****************
	@Id
	private long oid;
	private String d;
	// ****** Relations *****************
	@OneToOne(fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	public AEntity a;
	@OneToOne(fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	public CEntity c;
	@OneToMany(targetEntity = BEntity.class)
	public Set<BEntity> bs;

	@OneToMany(targetEntity = BEntity.class)
	@JoinColumn
	public Set<BEntity> otherBs;

	@OneToOne(mappedBy="d", fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	private EEntity e;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	private Blob blob;
	
	public String getD() {
		return d;
	}

	public void setD(String d) {
		this.d = d;
	}
	

	// ****** ID *****************
	public long getOid() {
		return oid;
	}

	public void setOid(long oid) {
		this.oid = oid;
	}


	public AEntity getA() {
		return a;
	}

	public void setA(AEntity a) {
		this.a = a;
	}

	public Set<BEntity> getBs() {
		return bs;
	}

	public void setBs(Set<BEntity> bs) {
		this.bs = bs;
	}

	public Set<BEntity> getOtherBs() {
		return otherBs;
	}

	public void setOtherBs(Set<BEntity> otherBs) {
		this.otherBs = otherBs;
	}

	public CEntity getC() {
		return c;
	}

	public void setC(CEntity c) {
		this.c = c;
	}

	public Blob getBlob() {
		return blob;
	}

	public void setBlob(Blob blob) {
		this.blob = blob;
	}

	public EEntity getE() {
		return e;
	}

	public void setE(EEntity e) {
		this.e = e;
	}
	
	
}
