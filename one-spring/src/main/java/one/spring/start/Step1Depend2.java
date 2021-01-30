package one.spring.start;

import org.springframework.beans.factory.annotation.Autowired;

public class Step1Depend2 {

    /**
     * @AutoWired注解等效于在xml中properties中指定，但是需要在beans-schema中指明default-autowire="byType"
     */
    @Autowired
    private Step1Depend1 depend1;

    public Step1Depend1 getDepend1() {
        return depend1;
    }

    public void setDepend1(Step1Depend1 depend1) {
        this.depend1 = depend1;
    }
}
