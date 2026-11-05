package com.oficioya.worker.mapper;

import com.oficioya.worker.dto.WorkerTradeRateRequest;
import com.oficioya.worker.dto.WorkerTradeRequest;
import com.oficioya.worker.dto.WorkerTradeResponse;
import com.oficioya.worker.model.TradeModel;
import com.oficioya.worker.model.WorkerTradeModel;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = TradeDtoMapper.class,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface WorkerTradeDtoMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "trade", source = "tradeId")
    WorkerTradeModel toModel(WorkerTradeRequest request);

    // Solo cambia la tarifa: el oficio asociado no se toca
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "trade", ignore = true)
    WorkerTradeModel toModel(WorkerTradeRateRequest request);

    WorkerTradeResponse toResponse(WorkerTradeModel model);

    // El request solo trae el id del oficio; el service lo carga y valida que esté activo
    default TradeModel tradeFromId(Long tradeId) {
        if (tradeId == null) {
            return null;
        }
        TradeModel trade = new TradeModel();
        trade.setId(tradeId);
        return trade;
    }
}
