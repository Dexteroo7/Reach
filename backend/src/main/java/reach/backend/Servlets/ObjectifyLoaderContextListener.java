package reach.backend.Servlets;

import com.google.appengine.api.ThreadManager;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Subclass;

import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * This class processes the classpath for classes with the @Entity or @Subclass annotations from Objectify
 * and registers them with the ObjectifyFactory, it is multi-threaded uses a prebuilt list of classes to process
 * created by the Reflections library at compile time and works very fast!
 */
public class ObjectifyLoaderContextListener implements ServletContextListener {
    private static final Logger logger = Logger.getLogger(ObjectifyLoaderContextListener.class.getName());

    private final Set<Class<?>> entities;

    public ObjectifyLoaderContextListener() {
        this.entities = new HashSet<>();
    }

    @Override
    public void contextInitialized(@Nonnull final ServletContextEvent sce) {

        final ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setUrls(ClasspathHelper.forPackage(""));
        final ExecutorService es = Executors.newCachedThreadPool(ThreadManager.currentRequestThreadFactory());
        cb.setExecutorService(es);
        final Reflections r = new Reflections(cb);
        this.entities.addAll(r.getTypesAnnotatedWith(Entity.class));
        this.entities.addAll(r.getTypesAnnotatedWith(Subclass.class));
        es.shutdown();
        final ObjectifyFactory of = ObjectifyService.factory();
        for (final Class<?> cls : this.entities) {
            of.register(cls);
            logger.info("Registered {} with Objectify");
        }
    }

    @Override
    public void contextDestroyed(@Nonnull final ServletContextEvent sce) {
        /* this is intentionally empty */
    }
}