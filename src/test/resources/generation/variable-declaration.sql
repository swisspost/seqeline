CREATE OR REPLACE PACKAGE BODY pkg IS
    PROCEDURE pr(p NUMBER) IS
        a number;
    BEGIN
        COMMIT;
    END;
END;