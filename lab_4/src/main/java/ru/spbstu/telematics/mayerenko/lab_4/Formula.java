package ru.spbstu.telematics.mayerenko.lab_4;

import java.util.LinkedList;
import java.util.Queue;

import java.lang.Math;

/**
 * Normal Polish Notation Parser
 * Парсер формулы в прямой польской записи со скобками
 */
public class Formula {
    
    /** Строковое представление формулы в прямой польской записи */
    private String _formula;

    /** Символ переменной функции, которую реализует формула */
    private String _variable;

    /** Синтаксическое дерево формулы, представленное массивом*/
    private Queue<Operand> _syntaxTree;

    /** Массив строковых представлений операций */
    private static String[] reservedNames = 
    {
        "*", "/", "+", "-", "sqr", "sqrt", "pow", "sin", "cos", "tan", "cot", "exp", "log", "pi"
    };

    /** Регулярное выражение для проверки формата числа типа double */
    private static String doubleFormatRegexp = 
    "^(-?)((((0?\\.)|([1-9]\\d*\\.))\\d*$)|([1-9]+\\d*\\.?\\d*e[+-]?[1-9]+$)|(0$|([1-9]+\\d*$)))";

    /** Типы операндов в формуле */
    private enum OperandType {
        /** Переменная функции*/
        VAR,
        /** Числовая константа */
        CONST,
        /** Умножение */
        MUL,
        /** Деление */
        DIV,
        /** Сложение */
        SUM,
        /** Вычитание */
        SUB,
        /** Квадрат */
        SQR,
        /** Квадратный корень */
        SQRT,
        /** Возведение в степень */
        POW,
        /** Синус */
        SIN,
        /** Косинус */
        COS,
        /** Тангенс */
        TAN,
        /** Котангенс */
        COT,
        /** Експонента */
        EXP,
        /** Натуральный логарифм */
        LOG
    }

    /** Класс операнда */
    private class Operand {
        /** Тип операнда */
        private OperandType _type;

        /** 
         * 1) Значение операнда, если он является числовой константой <br>
         * 2) Количество собственных операндов, если опернд является подформулой
         */
        private double _value;

        /**
         * Конструктор класса операнда
         * @param type - тип операнда
         * @param value - значение операнда, если он является числовой константой, иначе значение не учитывается
         */
        public Operand(OperandType type, double value) {
            _type = type;
            _value = value;
        }

        public OperandType getType() {
            return _type;
        }

        public double getValue() {
            return _value;
        }
    }

    /**
     * Сконструировать класс парсера формул
     * Класс обрабатывает формулы в прямой польской нотации вида: a + b <=> (+ a b)
     * Доступные операции: *, /, +, -, sqr, sqrt, pow, sin, cos, tan, cot, exp, log
     * Доступная константа: pi
     * @param formula - строковое представление формулы в прямой польской записи
     * @param variable - cимвол переменной функции, которую реализует формула
     */
    public Formula(String formula, String variable) {

        if (formula == null) {
            throw new NullPointerException("Formula is undefined");
        }

        _formula = formula;
        _variable = variable;
        _syntaxTree = new LinkedList<Operand>();

        int parenthesesStatus = checkParentheses(formula);

        if (parenthesesStatus == 0) {
            try {
                parse(_formula);
            }
            catch (RuntimeException e) {
                throw e;
            }
        } else if (parenthesesStatus == -1) {
            throw new RuntimeException("Unclosed parentheses");
        } else {
            throw new RuntimeException("Extra closing parenthesis at " + parenthesesStatus);
        }
    }

    /**
     * Вычислить значение фуекции в точке x
     * @param x - аргумент функции
     * @return значие функции в точке {@code x}
     */
    public double f(double x) {
        Operand operation = _syntaxTree.poll();
        OperandType type = operation.getType();
        int operandsCount = (int) operation.getValue();
        double[] operandValues = new double[operandsCount];

        for (int i = 0; i < operandsCount; i++) {
            Operand nextOperand = _syntaxTree.peek();
            OperandType nextOperandType = nextOperand.getType();
            if (nextOperandType == OperandType.CONST) {
                operandValues[i] = nextOperand.getValue();
                _syntaxTree.poll();
            } else if (nextOperandType == OperandType.VAR) {
                operandValues[i] = x;
                _syntaxTree.poll();
            }
            else {
                operandValues[i] = f(x);
            } 
        }

        double result = 0.D;
        switch (type) {
            case MUL:
                result = 1;
                for (double d : operandValues) result *= d;
                break;
            case DIV:
                if (Double.compare(operandValues[1], 0.D) == 0) {
                    throw new ArithmeticException("Division by zero");
                } 
                result = operandValues[0] / operandValues[1];
                break;
            case SUM:
                result = 0;
                for (double d : operandValues) result += d;
                break;
            case SUB:
                result = operandValues[0] - operandValues[1];
                break;
            case SQR:
                result = Math.pow(operandValues[0], 2.D);
                break;
            case SQRT:
                if (Double.compare(operandValues[0], 0.D) < 0) {
                    throw new ArithmeticException("Square root of a negative value");
                }
                result = Math.sqrt(operandValues[0]);
                break;
            case POW:
                result = Math.pow(operandValues[0], operandValues[1]);
                break;
            case SIN:
                result = Math.sin(operandValues[0]);
                break;
            case COS:
                result = Math.cos(operandValues[0]);
                break;
            case TAN:
                result = Math.tan(operandValues[0]);
                break;
            case COT:
                result = 1 / Math.tan(operandValues[0]);
                break;
            case EXP:
                result = Math.exp(operandValues[0]);
                break;
            case LOG:
                if (Double.compare(operandValues[0], 0.D) < 0) {
                    throw new ArithmeticException("Logarithm of a negative value");
                }
                result = Math.log(operandValues[0]);
                break;
            default:
                break;
        }

        return result;
    }

    /**
     * Проверка правильности скобочной структуры
     * @param expression - выражение со скобочной структурой
     * @return 0, если структура правильна; -1, если не хватает закрывающей скобки;
     * число i, если на i-й позиции лишняя закрывающая скобка
     */
    private int checkParentheses(String expression) {

        if (!expression.contains("(") && !expression.contains(")")) {
            return -1;
        }

        int parenthesesCount = 0;
        int length = expression.length();

        for (int i = 0; i < length; i++) {
            switch (expression.charAt(i)) {
                case '(':
                    parenthesesCount++;
                    break;
                case ')':
                    parenthesesCount--;
                    break;            
                default:
                    break;
            }

            if (parenthesesCount < 0) {
                return i; // Лишняя закрывающая скобка
            }
        }

        if (parenthesesCount == 0) {
            return 0; // Скобочная структура правильна
        } else {
            return -1; // Не хватает закрывающей скобки
        }
    }

    /**
     * Сделать синтаксический разбор формулы в строковом представлении.
     * В результате выполнения создаётся синтаксическое дерево формулы {@code__syntaxTree}
     * @param formula - строковое представление формулы в прямой польской записи <br>
     * 
     * 1) Пример: (* pi (sqr x)) <=> pi*x^2<br>
     * 2) Пример: (+ (/ (* 2.3 x) (log x)) (sin x) 8) <=> 2.3 * x / log(x) + (sin(x) + 8
     */
    private void parse(String formula) {
        formula = formula.substring(1, formula.length() - 1); // Убрать внешние скобки
        formula = formula.strip(); // Очистить от внешних пробелов

        Queue<String> subformulas = new LinkedList<String>(); // Очередь подформул

        if (formula.indexOf(' ') == -1 || formula.isEmpty()) {
            throw new RuntimeException("Unexpected sequence");
        }

        // Значения полей нового операнда
        OperandType operandType = null;
        double operandValue = 0;

        String operandTypeName = formula.substring(0, formula.indexOf(' '));

        // Определить тип операции
        try {
            operandType = typeFromName(operandTypeName);
        }
        catch (RuntimeException e) {
            throw e;
        }

        // Убрать символ операции из формулы
        formula = formula.substring(formula.indexOf(' '), formula.length());
        formula = formula.stripLeading();

        char[] formulaCharArray = formula.toCharArray();
        int parenthesesCount = 0;

        // Выделить и сохранить в очереди операнды-подформулы
        for (int i = 0; i < formulaCharArray.length; i++) {
            if (formulaCharArray[i] == '(') {
                StringBuilder subformula = new StringBuilder();
                subformula.append(formulaCharArray[i]);
                formulaCharArray[i] = '#';
                parenthesesCount++;
                i++;

                while (parenthesesCount != 0) {
                    switch (formulaCharArray[i]) {
                        case '(':
                            parenthesesCount++;
                            break;
                        case ')':
                            parenthesesCount--;
                            break;            
                        default:
                            break;
                    }
                    subformula.append(formulaCharArray[i]);
                    formulaCharArray[i] = '#';
                    i++;
                }

                if ((i != formulaCharArray.length) && (formulaCharArray[i] != ' ')) {
                    throw new RuntimeException("Whitespace between brackets is missed");
                } else {
                    subformulas.add(subformula.toString());
                    continue;
                }
            }
        }

        formula = new String(formulaCharArray);

        // Разбить формулу на операнды и подсчитать и проверить их количество
        String[] operandList = formula.split("\s+");
        if (checkOperandNumber(operandType, operandList.length)) {
            operandValue = (double) operandList.length;
        } else {
            throw new RuntimeException("Incorrect number of operands in the " + operandTypeName + " operation");
        }

        // Занести операнды в синтаксическое дерево, рекрсивно разбирая подформулы
        _syntaxTree.add(new Operand(operandType, operandValue));

        for (String s : operandList) {
            if (!s.matches("#+")) { // Разбор простого операнда
                if (s.equals(_variable)) {
                    operandType = OperandType.VAR;
                    operandValue = 0.;
                } else if (s.equals(reservedNames[13])) {
                    operandType = OperandType.CONST;
                    operandValue = Math.PI;
                } else if (s.matches(doubleFormatRegexp)) {
                    operandType = OperandType.CONST;
                    operandValue = Double.valueOf(s);
                }
                else {
                    throw new RuntimeException("Invalid number format");
                }
                _syntaxTree.add(new Operand(operandType, operandValue)); 
            } else { // Разбор операнда-подформулы
                parse(subformulas.poll());
            }
        }

    }

    /**
     * Определить тип операции по строковому представлению
     * @param operandTypeName - строковое представление символа операции
     * @return Тип операции {@code OperandType}
     */
    private OperandType typeFromName(String operandTypeName) {
        if (operandTypeName.equals(reservedNames[0])) {
            return OperandType.MUL;
        } else if (operandTypeName.equals(reservedNames[1])) {
            return OperandType.DIV;
        } else if (operandTypeName.equals(reservedNames[2])) {
            return OperandType.SUM;
        } else if (operandTypeName.equals(reservedNames[3])) {
            return OperandType.SUB;
        } else if (operandTypeName.equals(reservedNames[4])) {
            return OperandType.SQR;
        } else if (operandTypeName.equals(reservedNames[5])) {
            return OperandType.SQRT;
        } else if (operandTypeName.equals(reservedNames[6])) {
            return OperandType.POW;
        } else if (operandTypeName.equals(reservedNames[7])) {
            return OperandType.SIN;
        } else if (operandTypeName.equals(reservedNames[8])) {
            return OperandType.COS;
        } else if (operandTypeName.equals(reservedNames[9])) {
            return OperandType.TAN;
        } else if (operandTypeName.equals(reservedNames[10])) {
            return OperandType.COT;
        } else if (operandTypeName.equals(reservedNames[11])) {
            return OperandType.EXP;
        } else if (operandTypeName.equals(reservedNames[12])) {
            return OperandType.LOG;
        } else {
            throw new RuntimeException("Unknown operation type");
        }
    }

    /**
     * Проверить, правильное ли количество операндов имеет операция
     * @param type - тип операции
     * @param operandsCount - количество операндов операции
     * @return - {@code true}, если количество операндов верно, {@code false} в обратном случае
     */
    private boolean checkOperandNumber(OperandType type, int operandsCount) {
        if (type == OperandType.DIV || type == OperandType.SUB || type == OperandType.POW) {
            return (boolean) (operandsCount == 2);
        } else if (type == OperandType.MUL || type == OperandType.SUM) {
            return (boolean) (operandsCount >= 2);
        } else {
            return (boolean) (operandsCount == 1);
        }
    }
}