package com.expensetracker.dao;

import com.expensetracker.config.DBConnection;
import com.expensetracker.model.Expense;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object (DAO) providing CRUD operations
 * for the 'expenses' table using JDBC PreparedStatements.
 *
 * Responsibilities:
 * - Insert expenses
 * - Retrieve expenses
 * - Update expenses
 * - Delete expenses
 * - Calculate category-wise spending totals
 */
public class ExpenseDAO {

    /**
     * Inserts a new Expense into the database.
     *
     * @param expense Expense model instance
     * @return Generated database ID, or -1 if the operation fails
     */
    public int insert(Expense expense) {

        if (expense == null) {
            return -1;
        }

        String sql =
                "INSERT INTO expenses " +
                "(amount, category, description, date) " +
                "VALUES (?, ?, ?, ?)";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            pstmt.setDouble(1, expense.getAmount());
            pstmt.setString(2, expense.getCategory());
            pstmt.setString(3, expense.getDescription());
            pstmt.setString(4, expense.getDate().toString());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                return -1;
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {

                if (rs.next()) {

                    int generatedId = rs.getInt(1);

                    expense.setId(generatedId);

                    return generatedId;
                }
            }

        } catch (SQLException e) {

            System.err.println(
                    "[ExpenseDAO] Insert operation failed: "
                            + e.getMessage()
            );
        }

        return -1;
    }

    /**
     * Retrieves all expenses from the database.
     *
     * Expenses are ordered by date descending,
     * followed by ID descending.
     *
     * @return List of Expense objects
     */
    public List<Expense> getAllExpenses() {

        List<Expense> expenses = new ArrayList<>();

        String sql =
                "SELECT id, amount, category, description, date " +
                "FROM expenses " +
                "ORDER BY date DESC, id DESC";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {

            while (rs.next()) {

                int id = rs.getInt("id");

                double amount = rs.getDouble("amount");

                String category =
                        rs.getString("category");

                String description =
                        rs.getString("description");

                String dateString =
                        rs.getString("date");

                LocalDate date =
                        LocalDate.parse(dateString);

                expenses.add(
                        new Expense(
                                id,
                                amount,
                                category,
                                description,
                                date
                        )
                );
            }

        } catch (SQLException | RuntimeException e) {

            System.err.println(
                    "[ExpenseDAO] GetAll operation failed: "
                            + e.getMessage()
            );
        }

        return expenses;
    }

    /**
     * Updates an existing expense record.
     *
     * @param expense Expense record containing a valid ID
     * @return true if the record was updated, otherwise false
     */
    public boolean update(Expense expense) {

        if (expense == null || expense.getId() == null) {
            return false;
        }

        String sql =
                "UPDATE expenses SET " +
                "amount = ?, " +
                "category = ?, " +
                "description = ?, " +
                "date = ? " +
                "WHERE id = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setDouble(1, expense.getAmount());
            pstmt.setString(2, expense.getCategory());
            pstmt.setString(3, expense.getDescription());
            pstmt.setString(4, expense.getDate().toString());
            pstmt.setInt(5, expense.getId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println(
                    "[ExpenseDAO] Update operation failed: "
                            + e.getMessage()
            );
        }

        return false;
    }

    /**
     * Deletes an expense using its primary key ID.
     *
     * @param id Expense ID
     * @return true if the record was deleted, otherwise false
     */
    public boolean delete(int id) {

        String sql =
                "DELETE FROM expenses WHERE id = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, id);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println(
                    "[ExpenseDAO] Delete operation failed: "
                            + e.getMessage()
            );
        }

        return false;
    }

    /**
     * Calculates total spending grouped by category.
     *
     * SQL performs the SUM and GROUP BY operations.
     * Java stores the results in a HashMap.
     *
     * @return Map containing category names and total spending
     */
    public Map<String, Double> getCategoryTotals() {

        Map<String, Double> categoryMap =
                new HashMap<>();

        String sql =
                "SELECT category, SUM(amount) AS total " +
                "FROM expenses " +
                "GROUP BY category";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {

            while (rs.next()) {

                String category =
                        rs.getString("category");

                double total =
                        rs.getDouble("total");

                categoryMap.put(category, total);
            }

        } catch (SQLException e) {

            System.err.println(
                    "[ExpenseDAO] GetCategoryTotals failed: "
                            + e.getMessage()
            );
        }

        return categoryMap;
    }
}
