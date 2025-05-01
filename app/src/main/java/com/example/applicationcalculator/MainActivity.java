package com.example.applicationcalculator;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private TextView inputTextView;
    private StringBuilder currentExpression = new StringBuilder();
    private boolean isRadianMode = true;
    private DecimalFormat df = new DecimalFormat("#.########");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main); // Loads correct XML based on orientation

        inputTextView = findViewById(R.id.display);

        setCommonButtons();
        setScientificButtonsIfAvailable();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setCommonButtons() {
        int[] btnIds = new int[]{
                R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four,
                R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine,
                R.id.add, R.id.substract, R.id.divide, R.id.multiply,
                R.id.percentage, R.id.decimal, R.id.clear, R.id.equal
        };

        for (int id : btnIds) {
            Button button = findViewById(id);
            if (button != null) {
                button.setOnClickListener(v -> {
                    Button b = (Button) v;
                    String text = b.getText().toString();
                    handleCommonInput(text);
                });
            }
        }

        // Handle delete button separately
        Button deleteButton = findViewById(R.id.delete);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                if (currentExpression.length() > 0) {
                    currentExpression.deleteCharAt(currentExpression.length() - 1);
                    inputTextView.setText(currentExpression.toString());
                }
            });
        }
    }

    private void handleCommonInput(String text) {
        switch (text) {
            case "C":
                currentExpression.setLength(0);
                break;
            case "=":
                evaluateExpression();
                return;
            case "+":
            case "-":
            case "×": // Ensuring we handle the actual multiplication symbol
            case "÷": // Ensuring we handle the actual division symbol
            case "x": // Handle 'x' as multiplication too
                // Validate that we don't add consecutive operators
                if (isLastCharOperator() || currentExpression.length() == 0) {
                    // Don't allow operators at the beginning except for minus (negative numbers)
                    if (currentExpression.length() == 0 && !text.equals("-")) {
                        Toast.makeText(this, "Cannot start with this operator", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Don't allow consecutive operators
                    if (currentExpression.length() > 0) {
                        Toast.makeText(this, "Cannot add consecutive operators", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Make sure we normalize operators
                if (text.equals("×") || text.equals("x"))
                    currentExpression.append("*");
                else if (text.equals("÷"))
                    currentExpression.append("/");
                else
                    currentExpression.append(text);
                break;
            default:
                currentExpression.append(text);
        }
        inputTextView.setText(currentExpression.toString());
    }

    private boolean isLastCharOperator() {
        if (currentExpression.length() == 0) return false;

        char lastChar = currentExpression.charAt(currentExpression.length() - 1);
        return lastChar == '+' || lastChar == '-' || lastChar == '*' || lastChar == '/' ||
                lastChar == '×' || lastChar == '÷';
    }

    private void evaluateExpression() {
        try {
            if (currentExpression.length() == 0) {
                return; // Nothing to evaluate
            }

            // Check if expression ends with an operator
            if (isLastCharOperator()) {
                Toast.makeText(this, "Expression cannot end with an operator", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check for unbalanced parentheses
            if (!hasBalancedParentheses(currentExpression.toString())) {
                Toast.makeText(this, "Unbalanced parentheses", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save the original expression for error reporting
            String originalExpression = currentExpression.toString();

            // Normalize operators and prepare expression
            String expr = originalExpression
                    .replaceAll("×", "*")
                    .replaceAll("x", "*")
                    .replaceAll("÷", "/")
                    .replaceAll("%", "/100");

            // Perform basic evaluation
            double result = simpleEval(expr);

            // Format the result to avoid unnecessary decimal places
            String formattedResult;
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                formattedResult = String.format("%.0f", result);
            } else {
                formattedResult = df.format(result);
            }

            inputTextView.setText(formattedResult);
            currentExpression = new StringBuilder(formattedResult);
        } catch (Exception e) {
            inputTextView.setText("Error");
            currentExpression.setLength(0);
            // Show error details for debugging
            Toast.makeText(this, "Calculation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasBalancedParentheses(String expression) {
        int count = 0;
        for (char c : expression.toCharArray()) {
            if (c == '(') count++;
            if (c == ')') count--;
            if (count < 0) return false; // More closing than opening
        }
        return count == 0; // Should be balanced at the end
    }

    // Simple but robust evaluation function for basic operations
    private double simpleEval(String expression) {
        // Remove spaces if any
        expression = expression.replaceAll("\\s+", "");

        return evaluateExpression(expression);
    }

    private double evaluateExpression(String expression) {
        return new ExpressionEvaluator().evaluate(expression);
    }

    // Inner class for expression evaluation
    private class ExpressionEvaluator {
        private int pos = -1;
        private char ch;
        private String expr;

        public double evaluate(String expression) {
            expr = expression;
            pos = -1;
            nextChar();
            double x = parseExpression();
            if (pos < expr.length()) {
                throw new RuntimeException("Unexpected character: " + ch);
            }
            return x;
        }

        private void nextChar() {
            ch = (++pos < expr.length()) ? expr.charAt(pos) : '\0';
        }

        private boolean eat(char charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        private double parseExpression() {
            double x = parseTerm();
            while (true) {
                if (eat('+')) x += parseTerm();       // Addition
                else if (eat('-')) x -= parseTerm();  // Subtraction
                else return x;
            }
        }

        private double parseTerm() {
            double x = parseFactor();
            while (true) {
                if (eat('*')){ x *= parseFactor();}     // Multiplication
                else if (eat('/')) {                  // Division
                    double divisor = parseFactor();
                    if (divisor == 0) throw new ArithmeticException("Division by zero");
                    x /= divisor;
                }
                else return x;
            }
        }

        private double parseFactor() {
            // Check for plus or minus sign
            if (eat('+')) return parseFactor();       // Unary plus
            if (eat('-')) return -parseFactor();      // Unary minus

            double x;
            int startPos = this.pos;

            // Parse parentheses
            if (eat('(')) {
                x = parseExpression();
                if (!eat(')')) throw new RuntimeException("Missing closing parenthesis");
            }
            // Parse functions
            else if (Character.isLetter(ch)) {
                // Function name
                StringBuilder funcName = new StringBuilder();
                while (Character.isLetter(ch)) {
                    funcName.append(ch);
                    nextChar();
                }

                // Function handling
                switch (funcName.toString()) {
                    case "sin":
                        if (!eat('(')) throw new RuntimeException("Missing opening parenthesis after sin");
                        x = parseExpression();
                        if (!isRadianMode) x = Math.toRadians(x);
                        x = Math.sin(x);
                        if (!eat(')')) throw new RuntimeException("Missing closing parenthesis after sin argument");
                        break;
                    case "cos":
                        if (!eat('(')) throw new RuntimeException("Missing opening parenthesis after cos");
                        x = parseExpression();
                        if (!isRadianMode) x = Math.toRadians(x);
                        x = Math.cos(x);
                        if (!eat(')')) throw new RuntimeException("Missing closing parenthesis after cos argument");
                        break;
                    case "tan":
                        if (!eat('(')) throw new RuntimeException("Missing opening parenthesis after tan");
                        x = parseExpression();
                        if (!isRadianMode) x = Math.toRadians(x);
                        x = Math.tan(x);
                        if (!eat(')')) throw new RuntimeException("Missing closing parenthesis after tan argument");
                        break;
                    case "log":
                        if (!eat('(')) throw new RuntimeException("Missing opening parenthesis after log");
                        x = parseExpression();
                        if (x <= 0) throw new ArithmeticException("Cannot take log of non-positive number");
                        x = Math.log10(x);
                        if (!eat(')')) throw new RuntimeException("Missing closing parenthesis after log argument");
                        break;
                    case "sqrt":
                        if (!eat('(')) throw new RuntimeException("Missing opening parenthesis after sqrt");
                        x = parseExpression();
                        if (x < 0) throw new ArithmeticException("Cannot take square root of negative number");
                        x = Math.sqrt(x);
                        if (!eat(')')) throw new RuntimeException("Missing closing parenthesis after sqrt argument");
                        break;
                    default:
                        throw new RuntimeException("Unknown function: " + funcName);
                }
            }
            // Parse numbers
            else if (Character.isDigit(ch) || ch == '.') {
                StringBuilder sb = new StringBuilder();
                while (Character.isDigit(ch) || ch == '.') {
                    sb.append(ch);
                    nextChar();
                }
                try {
                    x = Double.parseDouble(sb.toString());
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid number format: " + sb.toString());
                }
            }
            else {
                throw new RuntimeException("Unexpected character: " + ch);
            }

            // Handle factorial
            if (eat('!')) {
                if (x < 0 || x != Math.floor(x))
                    throw new ArithmeticException("Cannot calculate factorial of negative or non-integer");
                x = factorial((int)x);
            }

            return x;
        }

        private double factorial(int n) {
            if (n == 0 || n == 1) return 1;

            double result = 1;
            for (int i = 2; i <= n; i++) {
                result *= i;
            }
            return result;
        }
    }

    private void setScientificButtonsIfAvailable() {
        int[] sciBtnIds = new int[]{
                R.id.sin, R.id.cos, R.id.tan, R.id.log,
                R.id.sqrt, R.id.factorial, R.id.deg, R.id.rad,
                R.id.para_open, R.id.para_close
        };

        for (int id : sciBtnIds) {
            View view = findViewById(id);
            if (view != null) {
                view.setOnClickListener(v -> {
                    Button b = (Button) v;
                    handleScientificInput(b.getText().toString());
                });
            }
        }
    }

    private void handleScientificInput(String text) {
        switch (text) {
            case "sin":
            case "cos":
            case "tan":
            case "log":
                currentExpression.append(text).append("(");
                break;
            case "√":
                currentExpression.append("sqrt(");
                break;
            case "!":
                // Check if there's a valid number before adding factorial
                if (currentExpression.length() > 0 && Character.isDigit(currentExpression.charAt(currentExpression.length() - 1))) {
                    currentExpression.append("!");
                } else {
                    Toast.makeText(this, "Factorial needs a number before it", Toast.LENGTH_SHORT).show();
                }
                break;
            case "(":
                // Allow opening parenthesis freely
                currentExpression.append(text);
                break;
            case ")":
                // Check for balanced parentheses before adding closing parenthesis
                int openCount = 0;
                int closeCount = 0;
                for (int i = 0; i < currentExpression.length(); i++) {
                    if (currentExpression.charAt(i) == '(') openCount++;
                    if (currentExpression.charAt(i) == ')') closeCount++;
                }
                if (openCount > closeCount) {
                    currentExpression.append(text);
                } else {
                    Toast.makeText(this, "Missing opening parenthesis", Toast.LENGTH_SHORT).show();
                }
                break;
            case "deg":
                isRadianMode = false;
                Toast.makeText(this, "Degree mode enabled", Toast.LENGTH_SHORT).show();
                break;
            case "rad":
                isRadianMode = true;
                Toast.makeText(this, "Radian mode enabled", Toast.LENGTH_SHORT).show();
                break;
        }

        inputTextView.setText(currentExpression.toString());
    }
}