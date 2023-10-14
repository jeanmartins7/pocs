public class TestClassSingleton {

    public static void main(String[] args) {
        ClassSingleton classSingleton1 = ClassSingleton.getInstance();

        System.out.println(classSingleton1.getInfo());
        System.out.println(classSingleton1.getOutraInfo());
        System.out.println("Utilizando o metodo de soma da classe singleton para somar 1 + 1 com o resultado: " + classSingleton1.soma(1, 1));
    }
}
