package com.martins.jean.estoquepreco.rabbitmq;

import com.martins.jean.estoquepreco.Utils.RabbitmqConstantes;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class RabbitMQConection {
    private static final String NOME_EXCHANGE = "amq.direct";
    private AmqpAdmin amqpAdmin;

    public RabbitMQConection(AmqpAdmin amqpAdmin){
        this.amqpAdmin = amqpAdmin;
    }

    //criando a fila
    private Queue fila(String nomeFila){
    return new Queue(nomeFila, true, false, false);
    }

    //criando exchangeDireta
    private DirectExchange trocaDireta(){
        return new DirectExchange(NOME_EXCHANGE);
    }

    //criando relacionamento
    private Binding relacionamento(Queue fila, DirectExchange troca){
        return new Binding(fila.getName(), Binding.DestinationType.QUEUE, troca.getName(), fila.getName(), null);
    }

    @PostConstruct
    private void adciona(){
       Queue filaEstoque =  this.fila(RabbitmqConstantes.FILA_ESTOQUE);
       Queue filaPreco =  this.fila(RabbitmqConstantes.FILA_PRECO);

       DirectExchange troca = this.trocaDireta();

       Binding ligacaoEstoque = this.relacionamento(filaEstoque, troca);
       Binding ligacaoPreco = this.relacionamento(filaPreco, troca);


       //criando a flina no rabbitmq
       this.amqpAdmin.declareQueue(filaEstoque);
       this.amqpAdmin.declareQueue(filaPreco);

       this.amqpAdmin.declareExchange(troca);

       this.amqpAdmin.declareBinding(ligacaoEstoque);
       this.amqpAdmin.declareBinding(ligacaoPreco);

    }

}
