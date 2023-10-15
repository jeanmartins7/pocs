
public final class ClassSingleton {

    private static  ClassSingleton INSTANCE;
    private String info = "Informacoes iniciais da class";
    private String novaInformacao = "uma nova informacao";

    //Classes Singleton possui construtor privado
    private ClassSingleton(){}

    public static ClassSingleton getInstance(){
        if(INSTANCE == null){
            INSTANCE = new ClassSingleton();
        }

        return INSTANCE;
    }

    public String getInfo() {
        return info;
    }

    public String getOutraInfo(){
        return novaInformacao;
    }

    public int soma(int a, int b){
        return a + b;
    }
}
