CREATE OR REPLACE PACKAGE BODY pkg IS
    PROCEDURE pr IS
        n NUMBER;
        CURSOR c IS SELECT a FROM t;
    BEGIN
        FETCH c INTO n;
    END;
END;