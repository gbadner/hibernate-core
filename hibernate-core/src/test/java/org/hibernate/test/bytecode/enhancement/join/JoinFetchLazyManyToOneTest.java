/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.join;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.Type;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@TestForIssue( jiraKey = "HHH-13134" )
@RunWith( BytecodeEnhancerRunner.class )
public class JoinFetchLazyManyToOneTest extends BaseCoreFunctionalTestCase {

	private final Clock clock = Clock.systemDefaultZone();

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				Product.class,
				ProductAdditionalSettings.class,
				ProductDescription.class,
				ProductName.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
		configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "false" );
	}

	@Test
	public void testJoinFetch() {
		doInHibernate(
				this::sessionFactory, session -> {
					Product product1 = new Product( clock );
					product1.setName( new ProductName( product1, "product 1: name 1" ) );
					product1.setName( new ProductName( product1, "product 1: name 2" ) );
					product1.setName( new ProductName( product1, "product 1: name 3" ) );
					product1.setDescription( new ProductDescription( product1, "something cool" ) );
					product1.setAdditionalSettings( new ProductAdditionalSettings( product1, true ) );

					Product product2 = new Product( clock );
					product2.setName( new ProductName( product2, "product 2: name 1" ) );
					product2.setDescription( new ProductDescription( product2, "also cool" ) );

					Product product3 = new Product( clock );
					product3.setName( new ProductName( product3, "product 3: name 1" ) );
					product3.setDescription( new ProductDescription( product3, "meh" ) );
					product3.setDescription( new ProductDescription( product3, "not meh" ) );
					product3.setAdditionalSettings( new ProductAdditionalSettings( product3, false ) );

					session.persist( product1 );
					session.persist( product2 );
					session.persist( product3 );
				}
		);

		doInHibernate(
				this::sessionFactory, s -> {
					String productsQuery = "SELECT product "
							+ "FROM Product product "
							+ "LEFT JOIN FETCH product.name productName "
							+ "LEFT JOIN FETCH product.description productDescription ";
					List<Product> products = s.createQuery( productsQuery, Product.class ).getResultList();
					int productSize = products.size();

					//log.info( String.format( "Products %d", products.size() ) );
				}
		);
	}

	@Entity(name = "Product")
	public static class Product
	{
		@Id
		@GeneratedValue
		private long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST})
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private ProductName name;

		@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private ProductDescription description;

		@OneToOne(mappedBy = "product", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private ProductAdditionalSettings additionalSettings;

		@Column
		private @NotNull LocalDateTime createdTime;

		public Product(final Clock clock)
		{
			this.createdTime = LocalDateTime.now(clock);
		}

		protected Product()
		{
		}

		public long getId()
		{
			return id;
		}

		public LocalDateTime getCreatedTime()
		{
			return createdTime;
		}

		public ProductName getName()
		{
			return name;
		}

		public void setName(final ProductName name)
		{
			if (this.name != null) {
				name.setPreviousName(this.name);
			}
			this.name = name;
		}

		public ProductDescription getDescription()
		{
			return description;
		}

		public void setDescription(final ProductDescription description)
		{
			if (this.description != null) {
				description.setPreviousDescription(this.description);
			}
			this.description = description;
		}

		public ProductAdditionalSettings getAdditionalSettings()
		{
			return additionalSettings;
		}

		public void setAdditionalSettings(final ProductAdditionalSettings additionalSettings)
		{
			if (this.additionalSettings != null) {
				additionalSettings.setPreviousSettings(this.additionalSettings);
			}
			this.additionalSettings = additionalSettings;
		}

		@Override
		public String toString()
		{
			return new StringJoiner(", ", Product.class.getSimpleName() + "[", "]")
					.add("id=" + id)
					.add("name=" + Optional.ofNullable( name ).map(ProductName::getName).orElse(null))
					.add("description=" + Optional.ofNullable(description).map(ProductDescription::getDescription).orElse(null))
					.add("createdTime=" + createdTime)
					.toString();
		}

	}

	@Entity(name = "ProductAdditionalSettings")
	public static class ProductAdditionalSettings
	{

		@Id
		@GeneratedValue
		private long id;

		@OneToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private Product product;

		@Column
		@NotNull
		private boolean flag;

		@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private ProductAdditionalSettings previousSettings;

		public ProductAdditionalSettings(final Product product, final boolean flag)
		{
			this.product = product;
			this.flag = flag;
		}

		protected ProductAdditionalSettings()
		{
		}

		public long getId()
		{
			return id;
		}

		public Product getProduct()
		{
			return product;
		}

		public boolean isFlag()
		{
			return flag;
		}

		public ProductAdditionalSettings getPreviousSettings()
		{
			return previousSettings;
		}

		public void setPreviousSettings(final ProductAdditionalSettings previousSettings)
		{
			this.previousSettings = previousSettings;
		}

		@Override
		public String toString()
		{
			return new StringJoiner(", ", ProductAdditionalSettings.class.getSimpleName() + "[", "]")
					.add( "id=" + id )
					.add( "flag=" + flag )
					.toString();
		}

	}

	@Entity(name = "ProductDescription")
	public static class ProductDescription
	{

		@Id
		@GeneratedValue
		private long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private Product product;

		@Column
		@Type(type = "org.hibernate.type.TextType")
		@NotNull
		private String description;

		@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private ProductDescription previousDescription;

		public ProductDescription(final Product product, final String description)
		{
			this.product = product;
			this.description = description;
		}

		protected ProductDescription()
		{
		}

		public long getId()
		{
			return id;
		}

		public Product getProduct()
		{
			return product;
		}

		public String getDescription()
		{
			return description;
		}

		public ProductDescription getPreviousDescription()
		{
			return previousDescription;
		}

		public void setPreviousDescription(final ProductDescription previousDescription)
		{
			this.previousDescription = previousDescription;
		}

		@Override
		public String toString()
		{
			return new StringJoiner(", ", ProductDescription.class.getSimpleName() + "[", "]")
					.add( "id=" + id )
					.add( "description='" + description + "'" )
					.toString();
		}

	}

	@Entity(name = "ProductName")
	public static class ProductName
	{

		@Id
		@GeneratedValue
		private long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private Product product;

		@Column
		@Type(type = "org.hibernate.type.TextType")
		@NotNull
		private String name;

		@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private ProductName previousName;

		public ProductName(final Product product, final String name)
		{
			this.product = product;
			this.name = name;
		}

		protected ProductName()
		{
		}

		public long getId()
		{
			return id;
		}

		public Product getProduct()
		{
			return product;
		}

		public String getName()
		{
			return name;
		}

		public ProductName getPreviousName()
		{
			return previousName;
		}

		public void setPreviousName(final ProductName previousName)
		{
			this.previousName = previousName;
		}

		@Override
		public String toString()
		{
			return new StringJoiner(", ", ProductName.class.getSimpleName() + "[", "]")
					.add( "id=" + id )
					.add( "name='" + name + "'" )
					.toString();
		}

	}

}
