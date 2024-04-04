CREATE OR REPLACE PACKAGE BODY pkg IS
    PROCEDURE pr(p t%ROWTYPE) IS
        n NUMBER;
    BEGIN
        n := p.a;
    END;
END;