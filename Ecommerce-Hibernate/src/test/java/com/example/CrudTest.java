package com.example;

import com.example.entity.*;
import com.example.util.HibernateUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CrudTest {

    @AfterAll
    static void tearDown() {
        HibernateUtil.shutdown();
    }

    @Test
    void insertAndFindCategoryWithProduct() {
        String name = "Cat-" + UUID.randomUUID();
        Long id;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            Category c = new Category(name, "Electronics category");
            Product p = new Product("Item-" + UUID.randomUUID(), new BigDecimal("9.99"), 5);
            c.addProduct(p);
            s.persist(c);
            tx.commit();
            id = c.getId();
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Category found = s.find(Category.class, id);
            assertNotNull(found);
            assertEquals(name, found.getName());
            assertEquals("Electronics category", found.getDescription());
            assertEquals(1, found.getProducts().size());
        }
    }

    @Test
    void createOrderWithMultipleDetails() {
        Long orderId;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            String u = "user-" + UUID.randomUUID();
            Users user = new Users(u, BCrypt.hashpw("pass123", BCrypt.gensalt()), u + "@x.com", Role.CUSTOMER);
            Product p1 = new Product("P1-" + UUID.randomUUID(), new BigDecimal("10.00"), 10);
            Product p2 = new Product("P2-" + UUID.randomUUID(), new BigDecimal("20.00"), 5);
            s.persist(p1);
            s.persist(p2);

            Orders o = new Orders();
            o.setOrderDate(LocalDateTime.now());
            o.setTotalAmount(new BigDecimal("50.00"));
            o.addDetail(new OrderDetails(p1, 1, p1.getPrice()));
            o.addDetail(new OrderDetails(p2, 2, p2.getPrice()));
            user.addOrder(o);

            s.persist(user);
            tx.commit();

            orderId = o.getId();
            assertNotNull(orderId);
            assertEquals(2, o.getDetails().size());
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Orders found = s.find(Orders.class, orderId);
            assertNotNull(found);
            assertEquals(new BigDecimal("50.00"), found.getTotalAmount());
            assertEquals(2, found.getDetails().size());
        }
    }

    @Test
    void fetchOrderAlongWithAssociatedUsersAndProducts() {
        Long orderId;
        String username = "fetchUser-" + UUID.randomUUID();
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            Users user = new Users(username, BCrypt.hashpw("secret", BCrypt.gensalt()), username + "@mail.com", Role.CUSTOMER);
            Product p = new Product("Monitor-" + UUID.randomUUID(), new BigDecimal("250.00"), 8);
            s.persist(p);

            Orders o = new Orders();
            o.setOrderDate(LocalDateTime.now());
            o.setTotalAmount(new BigDecimal("250.00"));
            o.addDetail(new OrderDetails(p, 1, p.getPrice()));
            user.addOrder(o);

            s.persist(user);
            tx.commit();
            orderId = o.getId();
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Orders o = s.createQuery(
                    "SELECT DISTINCT o FROM Orders o " +
                    "JOIN FETCH o.user " +
                    "JOIN FETCH o.details d " +
                    "JOIN FETCH d.product " +
                    "WHERE o.id = :id", Orders.class)
                    .setParameter("id", orderId)
                    .getSingleResult();

            assertNotNull(o);
            assertEquals(username, o.getUser().getUsername());
            assertEquals(1, o.getDetails().size());
            assertEquals(new BigDecimal("250.00"), o.getDetails().get(0).getProduct().getPrice());
        }
    }

    @Test
    void updateOperationsForCategoryAndProduct() {
        Long catId;
        Long prodId;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            Category cat = new Category("OldCategory-" + UUID.randomUUID(), "Old Desc");
            Product prod = new Product("OldProduct-" + UUID.randomUUID(), new BigDecimal("100.00"), 10);
            cat.addProduct(prod);
            s.persist(cat);
            tx.commit();
            catId = cat.getId();
            prodId = prod.getId();
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            Category cat = s.find(Category.class, catId);
            Product prod = s.find(Product.class, prodId);

            cat.setDescription("Updated Desc");
            prod.setPrice(new BigDecimal("150.00"));
            prod.setStockQuantity(20);
            tx.commit();
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Category cat = s.find(Category.class, catId);
            Product prod = s.find(Product.class, prodId);

            assertEquals("Updated Desc", cat.getDescription());
            assertEquals(new BigDecimal("150.00"), prod.getPrice());
            assertEquals(20, prod.getStockQuantity());
        }
    }

    @Test
    void softDeleteHidesProduct() {
        Long id;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            Product p = new Product("Temp-" + UUID.randomUUID(), new BigDecimal("1.00"), 1);
            s.persist(p);
            tx.commit();
            id = p.getId();
        }
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            s.remove(s.find(Product.class, id));
            tx.commit();
        }
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            assertNull(s.find(Product.class, id));
        }
        // Verify physical row is still present in database with deleted = true
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Object deletedFlag = s.createNativeQuery(
                    "SELECT deleted FROM product WHERE id = :id", Boolean.class)
                    .setParameter("id", id)
                    .getSingleResult();
            assertEquals(Boolean.TRUE, deletedFlag);
        }
    }

    @Test
    void namedQueryFindByCategory() {
        Long catId;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            Category cat = new Category("NamedQueryCategory-" + UUID.randomUUID(), "Test");
            Product p1 = new Product("NQ-Prod1-" + UUID.randomUUID(), new BigDecimal("25.00"), 5);
            Product p2 = new Product("NQ-Prod2-" + UUID.randomUUID(), new BigDecimal("35.00"), 10);
            cat.addProduct(p1);
            cat.addProduct(p2);
            s.persist(cat);
            tx.commit();
            catId = cat.getId();
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            List<Product> products = s.createNamedQuery("Product.findByCategory", Product.class)
                    .setParameter("categoryId", catId)
                    .getResultList();
            assertEquals(2, products.size());
        }
    }

    @Test
    void criteriaQueryByPrice() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            Product expensive = new Product("Luxury-" + UUID.randomUUID(), new BigDecimal("75000.00"), 2);
            Product budget = new Product("Budget-" + UUID.randomUUID(), new BigDecimal("5000.00"), 10);
            s.persist(expensive);
            s.persist(budget);
            tx.commit();
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            CriteriaBuilder cb = s.getCriteriaBuilder();
            CriteriaQuery<Product> cq = cb.createQuery(Product.class);
            Root<Product> root = cq.from(Product.class);
            cq.select(root).where(cb.greaterThan(root.get("price"), new BigDecimal("50000.00")));

            List<Product> list = s.createQuery(cq).getResultList();
            assertFalse(list.isEmpty());
            assertTrue(list.stream().allMatch(p -> p.getPrice().compareTo(new BigDecimal("50000.00")) > 0));
        }
    }

    @Test
    void paginationForProductListings() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            for (int i = 1; i <= 5; i++) {
                s.persist(new Product("PageItem-" + UUID.randomUUID(), new BigDecimal(i * 10), i));
            }
            tx.commit();
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            int pageSize = 2;
            List<Product> page1 = s.createQuery("FROM Product ORDER BY id", Product.class)
                    .setFirstResult(0)
                    .setMaxResults(pageSize)
                    .getResultList();
            assertEquals(pageSize, page1.size());

            List<Product> page2 = s.createQuery("FROM Product ORDER BY id", Product.class)
                    .setFirstResult(2)
                    .setMaxResults(pageSize)
                    .getResultList();
            assertEquals(pageSize, page2.size());
            assertNotEquals(page1.get(0).getId(), page2.get(0).getId());
        }
    }

    @Test
    void userPasswordHashingAndVerification() {
        String plainPassword = "SuperSecurePassword123!";
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        Long userId;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            String u = "bcryptUser-" + UUID.randomUUID();
            Users user = new Users(u, hashedPassword, u + "@auth.com", Role.ADMIN);
            s.persist(user);
            tx.commit();
            userId = user.getId();
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Users user = s.find(Users.class, userId);
            assertNotNull(user);
            assertNotEquals(plainPassword, user.getPassword());
            assertTrue(BCrypt.checkpw(plainPassword, user.getPassword()));
            assertFalse(BCrypt.checkpw("WrongPassword", user.getPassword()));
            assertEquals(Role.ADMIN, user.getRole());
        }
    }
}