CREATE OR REPLACE PACKAGE BODY pkg IS
    PROCEDURE pr(p NUMBER) IS
    BEGIN
        pkg2.pr2(a => p);
    END;
END;