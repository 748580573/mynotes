# JPA之EntityManager使用自定义SQL



## 执行修改语句语句

````java
    @Autowired
    private EntityManager entityManager;


    public int updateTest(String id,String name){
        String sql = "UPDATE test SET NAME = ? WHERE ID = ?";
        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter(1,name).setParameter(2,id);
        //开启事务，jpa中涉及到更新和删除的操作一般都要求开启事务
        Session session = entityManager.unwrap(Session.class);
        Transaction transaction = session.getTransaction();
        transaction.begin();
        int result = nativeQuery.executeUpdate();
        transaction.commit();
        return result;
    }
````

