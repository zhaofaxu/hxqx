package cn.stylefeng.guns.sys.modular.i18n.init;

import cn.stylefeng.guns.sys.modular.i18n.context.TranslationContext;
import cn.stylefeng.guns.sys.modular.i18n.dict.TranslationDict;
import cn.stylefeng.guns.sys.modular.i18n.entity.Translation;
import cn.stylefeng.guns.sys.modular.i18n.model.enums.TranslationEnum;
import cn.stylefeng.guns.sys.modular.i18n.service.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 参数配置 服务类
 * </p>
 *
 * @author stylefeng
 * @since 2019-06-20
 */
@Component
@Slf4j
public class TranslationInit implements CommandLineRunner {

    @Autowired
    private TranslationService translationService;

    @Override
    public void run(String... args) {

        //初始化所有的翻译字典
        List<Translation> list = translationService.list();

        ArrayList<TranslationDict> translationDicts = new ArrayList<>();

        if (list != null) {
            for (Translation translation : list) {
                TranslationDict translationDict = new TranslationDict();
                translationDict.setTranCode(translation.getTranCode());
                translationDict.setTranName(translation.getTranName());
                translationDict.setTranslationLanguages(TranslationEnum.valueOf(translation.getLanguages()));
                translationDict.setTranValue(translation.getTranValue());
                translationDicts.add(translationDict);
            }

            TranslationContext.init(translationDicts);
            log.info("初始化所有的翻译字典" + list.size() + "条！");
        }
    }
}
