CREATE OR REPLACE PACKAGE BODY test_pack IS
    PROCEDURE test_proc (test_param NUMBER) IS
    BEGIN
        test_pack2.test_proc2(test_param);
    END;
END;