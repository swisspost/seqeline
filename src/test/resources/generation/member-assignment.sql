CREATE OR REPLACE PACKAGE BODY pkg IS
    PROCEDURE pr(r t%ROWTYPE) IS
        r2 t2%ROWTYPE;
    BEGIN
        r2.a := r.b;
    END;
END;