CREATE OR REPLACE PACKAGE BODY pkg IS
    PROCEDURE pr IS
        r t%ROWTYPE;
        s t%ROWTYPE;
        CURSOR c IS SELECT a FROM t;
    BEGIN
        FOR r IN c LOOP
            s := r;
        END LOOP;
    END;
END;