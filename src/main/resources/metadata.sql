SELECT
    json_object(
            'relations' VALUE json_arrayagg(
                    json_object(
                            'name' VALUE table_name,
                            'description' VALUE table_comment,
                            'columns' VALUE json_arrayagg(
                                    json_object(
                                            'name' VALUE column_name,
                                            'description' VALUE column_comment,
                                            'type' VALUE data_type
                                            ABSENT ON NULL
                                    ) RETURNING CLOB
                                ) ABSENT ON NULL RETURNING CLOB
                    ) RETURNING CLOB
            ) RETURNING CLOB
    ) AS schema_structure
FROM
    (
        SELECT DISTINCT
            lower(col.table_name) as table_name,
            all_tab_comments.comments as table_comment,
            lower(col.column_name) as column_name,
            lower(col.data_type) as data_type,
            all_col_comments.comments as column_comment
        FROM
            all_tab_cols col
                LEFT JOIN all_col_comments ON
                    col.table_name = all_col_comments.table_name AND
                    col.column_name = all_col_comments.column_name
                LEFT JOIN all_tab_comments ON
                    col.table_name = all_tab_comments.table_name
        WHERE
            col.owner != 'SYS'
    )
GROUP BY
    table_name, table_comment

