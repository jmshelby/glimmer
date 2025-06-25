;;; Directory Local Variables
;;; For more information see (info "(emacs) Directory Variables")

((clojure-mode . (
                  ;;(cider-clojure-cli-aliases . "test:dev:jetty:log")
                  (cider-clojure-cli-aliases . "dev")

                  (eval . (progn
                            (setq cider-repl-require-ns-on-set t)
                            (setenv "force-refresh" "7")
                            (setenv "DATOMIC_DEV_LOCAL" "1"))))))
