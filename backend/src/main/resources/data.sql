-- ============================================================
-- ShelfWise ERP - Seed Data
-- Runs after Hibernate creates schema (defer-datasource-initialization: true)
-- ============================================================

INSERT INTO products (sku, name, category, price, cost, stock_qty, reorder_point,
                      shelf_life_days, expiry_date, demand_rate, price_change_count,
                      lag1_sales, lag2_sales, supplier_name, lead_time_days,
                      created_at, updated_at)
SELECT * FROM (VALUES
  ('SKU-001','Organic Whole Milk 1L','Dairy',2.49,1.20,120,30,14,CURRENT_DATE+5,8.5,2,7.2,8.1,'FreshFarms Co',2,NOW(),NOW()),
  ('SKU-002','Aged Cheddar 200g','Dairy',4.99,2.80,85,20,30,CURRENT_DATE+3,5.2,1,4.8,5.5,'DairyBest',3,NOW(),NOW()),
  ('SKU-003','Sourdough Bread','Bakery',3.29,1.50,60,15,7,CURRENT_DATE+2,12.0,0,11.5,12.3,'LocalBakery',1,NOW(),NOW()),
  ('SKU-004','Whole Grain Crackers','Bakery',2.79,1.10,200,40,180,CURRENT_DATE+90,3.8,0,3.5,4.0,'GrainCo',5,NOW(),NOW()),
  ('SKU-005','Greek Yogurt 500g','Dairy',3.49,1.80,95,25,21,CURRENT_DATE+6,6.3,1,5.8,6.7,'FreshFarms Co',2,NOW(),NOW()),
  ('SKU-006','Free Range Eggs x12','Protein',3.99,2.20,70,20,21,CURRENT_DATE+10,9.2,0,8.5,9.8,'EggCellent',1,NOW(),NOW()),
  ('SKU-007','Salmon Fillet 300g','Protein',8.99,5.50,40,10,5,CURRENT_DATE+4,4.1,3,3.9,4.5,'SeaCatch',1,NOW(),NOW()),
  ('SKU-008','Baby Spinach 150g','Produce',1.99,0.80,110,30,7,CURRENT_DATE+3,14.5,0,13.2,15.1,'GreenGrow',1,NOW(),NOW()),
  ('SKU-009','Chicken Breast 500g','Protein',5.49,3.20,55,15,7,CURRENT_DATE+5,7.8,1,7.1,8.2,'PoultryPrime',1,NOW(),NOW()),
  ('SKU-010','Orange Juice 1L','Beverages',2.99,1.40,130,35,14,CURRENT_DATE+8,10.1,0,9.5,10.8,'CitrusFresh',2,NOW(),NOW()),
  ('SKU-011','Butter 250g','Dairy',2.29,1.10,145,30,60,CURRENT_DATE+45,4.5,0,4.2,4.8,'DairyBest',3,NOW(),NOW()),
  ('SKU-012','Strawberries 400g','Produce',3.99,2.00,35,15,5,CURRENT_DATE+2,11.2,2,10.5,12.0,'BerryFarm',1,NOW(),NOW()),
  ('SKU-013','Avocado x3','Produce',4.49,2.50,50,12,7,CURRENT_DATE+4,5.9,0,5.3,6.4,'TropicalImports',2,NOW(),NOW()),
  ('SKU-014','Sparkling Water 6-pack','Beverages',4.29,2.00,200,50,365,CURRENT_DATE+365,6.7,0,6.2,7.1,'AquaPure',7,NOW(),NOW()),
  ('SKU-015','Dark Chocolate 100g','Snacks',2.49,1.00,180,40,365,CURRENT_DATE+300,3.3,1,3.0,3.6,'ChocoWorld',10,NOW(),NOW())
) AS v(sku,name,category,price,cost,stock_qty,reorder_point,shelf_life_days,
       expiry_date,demand_rate,price_change_count,lag1_sales,lag2_sales,
       supplier_name,lead_time_days,created_at,updated_at)
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = v.sku);

-- Seed a few sample B2B orders
INSERT INTO b2b_orders (order_reference, retailer_name, retailer_size, product_id,
                        product_name, quantity_ordered, unit_price, discount_applied,
                        total_value, payment_terms, status, created_at)
SELECT * FROM (VALUES
  ('B2B-2024-001','Metro Grocers','LARGE',1,'Organic Whole Milk 1L',500,2.12,0.15,1060.00,'NET30','CONFIRMED',NOW()),
  ('B2B-2024-002','Corner Mart','SMALL',3,'Sourdough Bread',100,2.80,0.15,280.00,'COD','DELIVERED',NOW()),
  ('B2B-2024-003','SuperMart Chain','ENTERPRISE',10,'Orange Juice 1L',2000,2.55,0.15,5100.00,'NET60','PENDING',NOW()),
  ('B2B-2024-004','FreshMart','MEDIUM',5,'Greek Yogurt 500g',300,2.97,0.15,891.00,'NET30','SHIPPED',NOW())
) AS v(order_reference,retailer_name,retailer_size,product_id,product_name,
       quantity_ordered,unit_price,discount_applied,total_value,payment_terms,status,created_at)
WHERE NOT EXISTS (SELECT 1 FROM b2b_orders WHERE order_reference = v.order_reference);

-- Seed default demo users
INSERT INTO users (username, email, password, full_name, role, avatar_url, created_at)
SELECT * FROM (VALUES
  ('admin','admin@shelfwise.ai','admin123','System Administrator','ADMIN','https://api.dicebear.com/7.x/avataaars/svg?seed=admin',NOW()),
  ('manager','manager@shelfwise.ai','manager123','Sarah Jenkins','STORE_MANAGER','https://api.dicebear.com/7.x/avataaars/svg?seed=sarah',NOW()),
  ('analyst','analyst@shelfwise.ai','analyst123','Alex Rivera','AI_ANALYST','https://api.dicebear.com/7.x/avataaars/svg?seed=alex',NOW()),
  ('clerk','clerk@shelfwise.ai','clerk123','Marcus Chen','INVENTORY_CLERK','https://api.dicebear.com/7.x/avataaars/svg?seed=marcus',NOW())
) AS v(username,email,password,full_name,role,avatar_url,created_at)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = v.username);

