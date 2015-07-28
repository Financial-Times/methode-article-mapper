# Class: methode_article_transformer
# vim: ts=4 sts=4 sw=4 et sr smartindent:
# This module manages methode_article_transformer
#
# Parameters:
#
# Actions:
#
# Requires:
#
# Sample Usage:
#
class methode_article_transformer {

    $jar_name = 'methode-article-transformer-service.jar'
    $dir_heap_dumps = "/var/log/apps/methode_article_transformer-heap-dumps"
    $maxHeap = hiera('max_heap_space', '1024m')
    $memOpts = "-Xmx$maxHeap"
    $heapDumpOpts = "-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$dir_heap_dumps"

    file { "heap-dumps-dir":
        path    => "${dir_heap_dumps}",
        owner   => "${module_name}",
        group   => "${module_name}",
        ensure  => 'directory',
        mode    => 744;
    }

    service {
    "${module_name}":
        ensure => 'stopped',
    }
    ->
    class { "common_pp_up": }
    ->
    class { "jdk":
            version => "1.8.0"
        }
    ->
    class { "dropwizard": }
    ->
    dropwizard::instance {
    "${module_name}":
        dropwizard_jar_file_src      => "${module_name}/$jar_name",
        dropwizard_conf_template_src => "${module_name}/config.yml.erb",
        healthcheck_url              => "http://localhost:8081/healthcheck",
        java_opts                    => "$memOpts $heapDumpOpts",
    }
    ->
    class { "${module_name}::monitoring": }

}

