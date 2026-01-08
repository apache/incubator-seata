-- Step 1: add new column
ALTER TABLE vgroup_table
ADD COLUMN cluster_name VARCHAR2(255);

-- Step 2: copy existing data
UPDATE vgroup_table
SET cluster_name = cluster;

-- Step 3: drop old constraint
ALTER TABLE vgroup_table
DROP CONSTRAINT uk_vgroup_namespace_cluster;

-- Step 4: drop old column
ALTER TABLE vgroup_table
DROP COLUMN cluster;

-- Step 5: add new constraint
ALTER TABLE vgroup_table
ADD CONSTRAINT uk_vgroup_namespace_cluster_name
UNIQUE (vGroup, namespace, cluster_name);
