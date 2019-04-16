package org.hibernate.test.instrument.cases;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import org.hibernate.test.instrument.domain.AEntity;
import org.hibernate.test.instrument.domain.BEntity;
import org.hibernate.test.instrument.domain.CEntity;
import org.hibernate.test.instrument.domain.DEntity;
import org.hibernate.test.instrument.domain.Document;
import org.hibernate.test.instrument.domain.EEntity;
import org.hibernate.test.instrument.domain.Folder;
import org.hibernate.test.instrument.domain.Owner;
import junit.framework.TestCase;

import static org.junit.Assert.fail;

public class TestFetchGroupsDefaultExecutable extends AbstractExecutable {
	protected String[] getResources() {
		return new String[] {"org/hibernate/test/instrument/domain/FetchGroupsDefault.hbm.xml"};
	}
	public void execute() {
		createData();
		Transaction lTx = null;
			try {
				System.out.println( "######################################################" );
				Session currentSession = getFactory().openSession();
				lTx = currentSession.beginTransaction();
				DEntity myD = (DEntity) currentSession.load( DEntity.class, new Long( 1 ) );
				System.out.println( "Property-Value: " + myD.getD() );
				System.out.println( "######################################################" );
				System.out.println( "Association-Value: " + myD.getA().getA() );
				System.out.println( "######################################################" );
				Blob lBlob = myD.getBlob();
				InputStream lIS = lBlob.getBinaryStream();
				ByteArrayOutputStream lBytesOut = new ByteArrayOutputStream();
				int len = 0;
				byte[] bytes = new byte[2000];
				while ( ( len = lIS.read( bytes ) ) > -1 ) {
					lBytesOut.write( bytes, 0, len );
				}
				lIS.close();
				lBytesOut.close();
				System.out.println( "Blob-Value: " + lBytesOut.toString() );
				lTx.commit();
				lTx = null;
				System.out.println( "######################################################" );
				System.out.println( currentSession.getTransaction().isActive() );
				System.out.println( "Association-Value: " + myD.getC().getC1() + " " + myD.getC().getC2() );
				System.out.println( "######################################################" );
				Set<BEntity> lBs = myD.getBs();
				for ( BEntity lB : lBs ) {
					System.out.println( lB.getB1() + " " + lB.getB2() );
				}
				System.out.println( "######################################################" );
				Set<BEntity> lOtherBs = myD.getOtherBs();
				for ( BEntity lOtherB : lOtherBs ) {
					System.out.println( lOtherB.getB1() + " " + lOtherB.getB2() );
				}
				System.out.println( "EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE" );
				System.out.println( "Association-Value (E): " + myD.getE().getE1() + " " + myD.getE().getE2() );
				System.out.println( "******************************************************" );
			}
			catch (Exception e) {
				System.out.println( "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" );
				e.printStackTrace();
				if ( lTx != null ) {
					lTx.rollback();
				}
				fail( "EXCEPTION" );
			}
	}

	private void createData() {
		// Create data
		Session lCurrentSession = getFactory().openSession();
		Transaction lTx = lCurrentSession.beginTransaction();

		DEntity d = new DEntity();
		d.setD("bla");
		d.setOid(1);

		byte[] lBytes = "agdfagdfagfgafgsfdgasfdgfgasdfgadsfgasfdgasfdgasdasfdg".getBytes();
		Blob lBlob = Hibernate.getLobCreator( lCurrentSession ).createBlob(lBytes);
		d.setBlob(lBlob);

		BEntity b1 = new BEntity();
		b1.setOid(1);
		b1.setB1(34);
		b1.setB2("huhu");

		BEntity b2 = new BEntity();
		b2.setOid(2);
		b2.setB1(37);
		b2.setB2("haha");

		Set<BEntity> lBs = new HashSet<BEntity>();
		lBs.add(b1);
		lBs.add(b2);
		d.setBs(lBs);

		AEntity a = new AEntity();
		a.setOid(1);
		a.setA("hihi");
		d.setA(a);

		EEntity e = new EEntity();
		e.setOid(17);
		e.setE1("Balu");
		e.setE2("B�r");

		e.setD(d);

		CEntity c = new CEntity();
		c.setOid(1);
		c.setC1( "ast" );
		c.setC2( "qwert" );
		c.setC3( "yxcv" );
		d.setC( c );

		lCurrentSession.save( b1 );
		lCurrentSession.save(b2);
		lCurrentSession.save(a);
		lCurrentSession.save(c);
		lCurrentSession.save(e);
		lCurrentSession.save(d);
		lTx.commit();
	}

}
