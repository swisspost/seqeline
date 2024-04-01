CREATE OR REPLACE PACKAGE BODY pkg IS
    PROCEDURE pr(p NUMBER) IS
        n NUMBER;
    BEGIN
        n := p;
    END;
END;