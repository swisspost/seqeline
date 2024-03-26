SELECT
    json_object(
            'relations' VALUE json_arrayagg(
                    json_object(
                            'name' VALUE relation_name,
                            'type' VALUE relation_type,
                            'comment' VALUE relation_comment,
                            'columns' VALUE json_arrayagg(
                                    json_object(
                                            'name' VALUE column_name,
                                            'comment' VALUE column_comment,
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
            lower(col.table_name) as relation_name,
            lower(all_objects.object_type) as relation_type,
            all_tab_comments.comments as relation_comment,
            lower(col.column_name) as column_name,
            lower(col.data_type) as data_type,
            all_col_comments.comments as column_comment
        FROM
            all_tab_cols col
                LEFT JOIN all_objects ON
                    col.table_name = all_objects.object_name
                LEFT JOIN all_col_comments ON
                    col.table_name = all_col_comments.table_name AND
                    col.column_name = all_col_comments.column_name
                LEFT JOIN all_tab_comments ON
                    col.table_name = all_tab_comments.table_name
        WHERE
            col.owner != 'SYS'
    )
GROUP BY
    relation_name, relation_comment, relation_type

