CREATE OR REPLACE PACKAGE BODY pkg IS
    PROCEDURE pr(p NUMBER) IS
        n NUMBER;
    BEGIN
        IF p > 0 THEN
            n := p;
        END IF;
    END;
END;