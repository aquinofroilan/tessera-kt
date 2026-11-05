ALTER TABLE product_categories
    ADD CONSTRAINT product_categories_id_org_unique UNIQUE (id, organization_id);

ALTER TABLE products
    DROP CONSTRAINT products_category_id_fkey;

ALTER TABLE products
    ADD CONSTRAINT products_category_org_fk
    FOREIGN KEY (category_id, organization_id)
    REFERENCES product_categories(id, organization_id)
    ON DELETE RESTRICT;
