CREATE OR REPLACE PACKAGE BODY pkg IS
    PROCEDURE pr(n number) IS
        r t%ROWTYPE;
        s t%ROWTYPE;
        CURSOR c(m number) IS SELECT a FROM t WHERE a > m;
    BEGIN
        FOR r IN c(n) LOOP
            s := r;
        END LOOP;
    END;
END;