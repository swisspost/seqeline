CREATE OR REPLACE PACKAGE BODY pack IS
    PROCEDURE pr (p NUMBER) IS
        a number;
    BEGIN
        COMMIT;
    END;
END;