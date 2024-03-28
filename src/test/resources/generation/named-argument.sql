CREATE OR REPLACE PACKAGE BODY pack IS
    PROCEDURE pr (p NUMBER) IS
    BEGIN
        pack2.pr2(a => p);
    END;
END;