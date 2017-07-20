/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.junit.Assert;

/**
 * This tests issues HHH-11624. The fix is also for HHH-10747 (and HHH-11476) and is a change on the enhanced setter.
 *
 * @author Luis Barreiro
 * @author Gail Badner
 */
public class LazyLoadingByEnhancerSetterPropAccessTestTask extends AbstractEnhancerTestTask {

    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Item.class};
    }

    public void prepare() {
        Configuration cfg = new Configuration();
        cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
        cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
        super.prepare( cfg );
        
        Session s = getFactory().openSession();
        s.beginTransaction();

        Item input = new Item();
        input.aName = "X";
        input.parameterMap = new HashMap<>();
        input.parameterMap.put( "aaa", "AAA" );
        input.parameterMap.put( "bbb", "BBB" );

        s.persist( input );

        s.getTransaction().commit();
        s.close();
    }

    public void execute() {
        Session s = getFactory().openSession();
        s.beginTransaction();

        // A parameterMap map is created with the class and is being compared to the persistent map (by the generated code) -- it shouldn't
        Item item = s.find( Item.class, "X" );

        s.getTransaction().commit();
        s.close();

        s = getFactory().openSession();
        s.beginTransaction();

        Item mergedItem = (Item) s.merge( item );

        s.getTransaction().commit();
        s.close();

        Assert.assertEquals( 2, mergedItem.parameterMap.size() );
    }

    protected void cleanup() {
    }

    @Entity
    @Table( name = "ITEM" )
    private static class Item {

        private String aName;

        private Map<String, String> parameterMap = new HashMap<>();

        @Id
        @Column( nullable = false )
        public String getName() {
            return aName;
        }
        public void setName(String name) {
            this.aName = name;
        }

        @ElementCollection( fetch = FetchType.EAGER )
        @CollectionTable( name = "STORED_INPUT_PARAMETER", joinColumns = @JoinColumn( name = "STORED_INPUT_ID" ) )
        @MapKeyColumn( name = "NAME" )
        @Lob
        @Column( name = "VALUE", length = 65535 )
        public Map<String, String> getParameters() {
            return parameterMap;
        }
        public void setParameters(Map<String, String> parameters) {
            this.parameterMap = parameters;
        }
    }
}
