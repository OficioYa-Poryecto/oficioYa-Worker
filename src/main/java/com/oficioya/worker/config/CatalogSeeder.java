package com.oficioya.worker.config;

import com.oficioya.worker.entity.Trade;
import com.oficioya.worker.entity.TradeCategory;
import com.oficioya.worker.repository.TradeCategoryRepository;
import com.oficioya.worker.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Carga el catálogo inicial (categorías y oficios definidos por la profesora) solo si no hay categorías
@Component
@Profile("!test")
public class CatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeeder.class);

    private record CategorySeed(String name, String detail, List<String> trades) {
    }

    private static final List<CategorySeed> CATALOG = List.of(
            new CategorySeed("Plomería y gas",
                    "Instalación, reparación y mantenimiento de tuberías, gas y griferías",
                    List.of("Plomero", "Técnico de gas", "Destapes", "Instalación de sanitarios y grifería")),
            new CategorySeed("Electricidad",
                    "Instalación y reparación de redes eléctricas, iluminación y tomas",
                    List.of("Electricista", "Instalación de iluminación", "Tomas y cableado")),
            new CategorySeed("Construcción y remodelación",
                    "Obra nueva, acabados y remodelación de espacios del hogar",
                    List.of("Maestro de obra", "Arquitecto", "Pintor", "Enchapes",
                            "Remodelación de baños y cocinas")),
            new CategorySeed("Carpintería y muebles",
                    "Fabricación, armado y reparación de muebles, puertas y estructuras de madera",
                    List.of("Carpintero", "Armado de closets y muebles", "Puertas y reparaciones")),
            new CategorySeed("Cerrajería y seguridad",
                    "Apertura, cambio y reparación de cerraduras y sistemas de acceso",
                    List.of("Cerrajero", "Cambio de guardas", "Aperturas")),
            new CategorySeed("Instalaciones y armado",
                    "Montaje de elementos del hogar como televisores, repisas y cortinas",
                    List.of("Televisores", "Repisas", "Espejos", "Cortinas", "Persianas")),
            new CategorySeed("Electrodomésticos",
                    "Instalación, reparación y mantenimiento de electrodomésticos del hogar",
                    List.of("Neveras", "Lavadoras", "Secadoras", "Estufas")),
            new CategorySeed("Aseo y limpieza",
                    "Limpieza general y profunda del hogar y cuidado de la ropa",
                    List.of("Aseo general", "Planchado", "Limpieza profunda")),
            new CategorySeed("Jardinería y exteriores",
                    "Mantenimiento y cuidado de jardines, árboles y zonas verdes",
                    List.of("Jardinero", "Poda", "Zonas verdes")),
            new CategorySeed("Cuidado de mascotas",
                    "Cuidado, paseo, aseo y adiestramiento de mascotas",
                    List.of("Paseador de perros", "Peluquería canina", "Cuidador a domicilio", "Adiestrador")),
            new CategorySeed("Servicios generales",
                    "Arreglos y tareas variadas del hogar que no encajan en otra categoría",
                    List.of("Todero", "Arreglos varios")));

    private final TradeCategoryRepository categoryRepository;
    private final TradeRepository tradeRepository;

    public CatalogSeeder(TradeCategoryRepository categoryRepository, TradeRepository tradeRepository) {
        this.categoryRepository = categoryRepository;
        this.tradeRepository = tradeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            log.info("Catalog already loaded, skipping seed");
            return;
        }
        int trades = 0;
        for (CategorySeed seed : CATALOG) {
            TradeCategory category = new TradeCategory();
            category.setName(seed.name());
            category.setDetail(seed.detail());
            category = categoryRepository.save(category);
            for (String tradeName : seed.trades()) {
                Trade trade = new Trade();
                trade.setName(tradeName);
                trade.setActive(true);
                trade.setCategory(category);
                tradeRepository.save(trade);
                trades++;
            }
        }
        log.info("Catalog seeded: {} categories, {} trades", CATALOG.size(), trades);
    }
}