DO $$
BEGIN
IF NOT EXISTS (
                   SELECT 1
                        FROM instance_role
                   WHERE name = 'ADMIN'
               )  THEN
                    INSERT INTO instance_role (identifier, name)
                    VALUES
                        ('0c24ae2a-a4c1-4347-8c51-6b5f716fe38f', 'ADMIN');
END IF;
IF NOT EXISTS (
                   SELECT 1
                        FROM instance_role
                   WHERE name = 'STANDARD'
               )  THEN
                    INSERT INTO instance_role (identifier, name)
                    VALUES
                            ('8289e388-4d2d-44e4-b8c2-8858988613a6', 'STANDARD');
END IF;
END $$;
