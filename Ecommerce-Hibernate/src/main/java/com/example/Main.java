package com.example;

import com.example.entity.*;
import com.example.util.HibernateUtil;
import jakarta.persistence.criteria.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            Long orderId = insertData();
            fetchOrder(orderId);
            productsByCategory();
            criteriaQuery();
            paginate();
            updateProduct();
            softDelete();
        } finally {
            HibernateUtil.shutdown();
        }
    }

    // ---------- CREATE ----------
    static Long insertData() {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Category electronics = new Category("Electronics", "Gadgets and devices");
            Product laptop = new Product("Laptop", new BigDecimal("55000.00"), 10);
            Product phone  = new Product("Phone",  new BigDecimal("20000.00"), 25);
            electronics.addProduct(laptop);
            electronics.addProduct(phone);
            session.persist(electronics);           // cascades to products

            Users user = new Users("nirnoy", BCrypt.hashpw("secret123", BCrypt.gensalt()),
                                   "nirnoy@example.com", Role.CUSTOMER);
            session.persist(user);

            Orders order = new Orders();
            order.setOrderDate(LocalDateTime.now());
            order.addDetail(new OrderDetails(laptop, 1, laptop.getPrice()));
            order.addDetail(new OrderDetails(phone, 2, phone.getPrice()));
            BigDecimal total = order.getDetails().stream()
                    .map(d -> d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setTotalAmount(total);
            user.addOrder(order);                   // cascades to order and its details

            tx.commit();
            return order.getId();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    // ---------- READ: order + user + products in one query ----------
    static void fetchOrder(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Orders o = session.createQuery(
                "SELECT DISTINCT o FROM Orders o " +
                "JOIN FETCH o.user " +
                "JOIN FETCH o.details d " +
                "JOIN FETCH d.product " +
                "WHERE o.id = :id", Orders.class)
                .setParameter("id", id)
                .getSingleResult();

            System.out.println("Order by " + o.getUser().getUsername() + ", total " + o.getTotalAmount());
            o.getDetails().forEach(d ->
                System.out.println("  " + d.getProduct().getName() + " x" + d.getQuantity()));
        }
    }

    // ---------- Named query (bonus) ----------
    static void productsByCategory() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Product> list = session.createNamedQuery("Product.findByCategory", Product.class)
                    .setParameter("categoryId", 1L)
                    .getResultList();
            list.forEach(p -> System.out.println("Named query: " + p.getName()));
        }
    }

    // ---------- CriteriaBuilder (bonus) ----------
    static void criteriaQuery() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Product> cq = cb.createQuery(Product.class);
            Root<Product> root = cq.from(Product.class);
            cq.select(root).where(cb.greaterThan(root.get("price"), new BigDecimal("30000")));
            session.createQuery(cq).getResultList()
                   .forEach(p -> System.out.println("Criteria: " + p.getName()));
        }
    }

    // ---------- Pagination (bonus) ----------
    static void paginate() {
        int page = 0, size = 1;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Product> pageItems = session.createQuery("FROM Product ORDER BY id", Product.class)
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();
            pageItems.forEach(p -> System.out.println("Page " + page + ": " + p.getName()));
        }
    }

    // ---------- UPDATE ----------
    static void updateProduct() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Product p = session.find(Product.class, 1L);
            p.setPrice(new BigDecimal("52000.00"));   // no save call needed: Hibernate detects the change
            tx.commit();
        }
    }

    // ---------- DELETE (soft) ----------
    static void softDelete() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Product p = session.find(Product.class, 2L);
            session.remove(p);                        // runs UPDATE ... SET deleted = true
            tx.commit();
        }
    }
}